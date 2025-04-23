# main.py

import torch
import torch.nn as nn
import numpy as np
import os
import pickle
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Optional
import uvicorn

# --- Pydantic 模型用于请求体校验 ---
# 这些模型定义了传入 JSON 数据的预期结构。

class Header(BaseModel):
    ctl: int
    dataCategory: int # 数据类别
    dataLen: int      # 数据长度
    prefix: int       # 前缀
    timestamp: int    # 时间戳
    ver: int          # 版本

class Position(BaseModel):
    elevation: int    # 海拔
    latitude: float   # 纬度
    longitude: float  # 经度

class DestLocation(BaseModel):
    latitude: float   # 纬度
    longitude: float  # 经度

class Body(BaseModel):
    destLocation: DestLocation # 目标位置
    engineTorque: int          # 发动机扭矩
    ext: Dict = Field(default_factory=dict) # 扩展字段，允许为空或有内容的字典
    heading: float             # 航向角
    messageId: int             # 消息 ID
    passPoints: List = Field(default_factory=list) # 途经点，允许为空列表
    passPointsNum: int         # 途经点数量
    position: Position         # 当前位置
    steeringAngle: int         # 方向盘转角
    tapPos: int                # 油门踏板位置 (假设)
    timestampGNSS: int         # GNSS 时间戳
    vehicleId: str             # 车辆 ID
    velocityGNSS: float        # GNSS 速度

class Record(BaseModel):
    header: Header # 消息头
    body: Body     # 消息体

# --- 特征解析函数 (针对 Pydantic 模型修改) ---
def parse_record_from_model(record: Record):
    """
    从 Pydantic Record 模型中解析特征和车辆 ID。

    Args:
        record (Record): 包含请求数据的 Pydantic 模型实例。

    Returns:
        tuple: 包含以下内容的元组：
            - list: 数值特征列表。
            - str: 车辆 ID。
    """
    header = record.header
    body = record.body
    features = []
    features.append(header.timestamp)
    features.append(body.velocityGNSS)
    features.append(body.position.longitude)
    features.append(body.position.latitude)
    features.append(body.position.elevation)
    features.append(body.heading)
    features.append(body.tapPos)
    features.append(body.steeringAngle)
    features.append(body.engineTorque)
    features.append(body.timestampGNSS)
    features.append(body.messageId)
    features.append(body.passPointsNum)

    vehicle_id = body.vehicleId
    return features, vehicle_id

# --- StandardScaler 类 (未改变) ---
class StandardScaler(object):
    """
    通过移除均值并将特征缩放到单位方差来进行标准化。
    """
    def __init__(self):
        self.mean = None # 均值
        self.std = None  # 标准差

    def fit(self, data):
        """
        计算用于后续缩放的均值和标准差。

        Args:
            data (np.ndarray): 用于计算均值和标准差的数据。
        """
        self.mean = np.mean(data, axis=0)
        self.std = np.std(data, axis=0)
        # 避免除以零
        self.std[self.std == 0] = 1.0

    def transform(self, data):
        """
        通过中心化和缩放执行标准化。

        Args:
            data (np.ndarray): 要转换的数据。

        Returns:
            np.ndarray: 转换后的数据。
        """
        if self.mean is None or self.std is None:
            raise RuntimeError("缩放器 (Scaler) 尚未拟合 (fit)。")
        return (data - self.mean) / self.std

    def fit_transform(self, data):
        """
        拟合数据，然后转换它。

        Args:
            data (np.ndarray): 要拟合和转换的数据。

        Returns:
            np.ndarray: 转换后的数据。
        """
        self.fit(data)
        return self.transform(data)

# --- PyTorch 模型定义 (未改变) ---
class DyT(nn.Module):
    """动态阈值层 (Dynamic Thresholding Layer)。"""
    def __init__(self, input_dim, init_alpha=0.01):
        super(DyT, self).__init__()
        self.alpha = nn.Parameter(torch.ones(input_dim) * init_alpha)
        self.gamma = nn.Parameter(torch.ones(input_dim))
        self.beta = nn.Parameter(torch.zeros(input_dim))

    def forward(self, x):
        alpha = self.alpha.view(1, 1, -1)
        gamma = self.gamma.view(1, 1, -1)
        beta = self.beta.view(1, 1, -1)
        return gamma * torch.tanh(alpha * x) + beta

class CustomTransformerEncoderLayer(nn.Module):
    """自定义 Transformer 编码器层，使用 DyT 替代 LayerNorm。"""
    def __init__(self, d_model, nhead, dim_feedforward=2048, dropout=0.1, init_alpha=0.01):
        super(CustomTransformerEncoderLayer, self).__init__()
        # 设置 batch_first=True
        self.self_attn = nn.MultiheadAttention(d_model, nhead, dropout=dropout, batch_first=True)
        self.dyt_attn = DyT(d_model, init_alpha=init_alpha)
        self.dropout1 = nn.Dropout(dropout)
        self.dyt_ffn = DyT(d_model, init_alpha=init_alpha)
        self.ffn = nn.Sequential(
            nn.Linear(d_model, dim_feedforward),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(dim_feedforward, d_model)
        )
        self.dropout2 = nn.Dropout(dropout)

    def forward(self, src):
        # src: (batch_size, seq_len, d_model) 因为 batch_first=True
        src2 = self.dyt_attn(src)
        attn_output, _ = self.self_attn(src2, src2, src2)
        src = src + self.dropout1(attn_output)
        src2 = self.dyt_ffn(src)
        src2 = self.ffn(src2)
        src = src + self.dropout2(src2)
        return src

class CustomTransformerEncoder(nn.Module):
    """自定义 Transformer 编码器层的堆叠。"""
    def __init__(self, encoder_layer, num_layers):
        super(CustomTransformerEncoder, self).__init__()
        # 使用 ModuleList 来正确注册层
        self.layers = nn.ModuleList([
            CustomTransformerEncoderLayer(
                d_model=encoder_layer.self_attn.embed_dim,
                nhead=encoder_layer.self_attn.num_heads,
                dim_feedforward=encoder_layer.ffn[0].out_features, # 推断 dim_feedforward
                dropout=encoder_layer.dropout1.p, # 推断 dropout
                init_alpha=0.01 # 假设相同的 init_alpha，如果需要则调整
            ) for _ in range(num_layers)
        ])
        self.num_layers = num_layers

    def forward(self, src):
        # src: (batch_size, seq_len, d_model)
        output = src
        for mod in self.layers:
            output = mod(output)
        return output


class TransformerAutoencoder(nn.Module):
    """使用自定义层的 Transformer 自编码器模型。"""
    def __init__(self, input_dim, hidden_dim, n_layers, n_heads, dropout=0.1,
                 dim_feedforward=2048, init_alpha=0.01, max_seq_len=256):
        super(TransformerAutoencoder, self).__init__()
        self.input_norm = nn.LayerNorm(input_dim)
        self.linear_in = nn.Linear(input_dim, hidden_dim)
        # 如果位置嵌入不打算学习，则分离它，或确保它被正确注册
        # 调整形状以适应 batch_first=True
        self.pos_embedding = nn.Parameter(torch.randn(1, max_seq_len, hidden_dim))
        encoder_layer = CustomTransformerEncoderLayer(
            d_model=hidden_dim,
            nhead=n_heads,
            dim_feedforward=dim_feedforward,
            dropout=dropout,
            init_alpha=init_alpha
        )
        self.encoder = CustomTransformerEncoder(encoder_layer, num_layers=n_layers)
        self.decoder = nn.Linear(hidden_dim, input_dim)
        self.hidden_dim = hidden_dim # 存储 hidden_dim

    def forward(self, x):
        # x: (batch_size, seq_len, input_dim)
        batch_size, seq_len, _ = x.size()

        # 在归一化之前确保输入张量具有正确的维度
        if x.dim() != 3:
             raise ValueError(f"期望输入张量具有 3 个维度 (batch_size, seq_len, input_dim)，但得到 {x.dim()} 个维度。")

        x = self.input_norm(x)
        x = self.linear_in(x)  # (batch_size, seq_len, hidden_dim)

        # 添加位置嵌入
        # 如有必要，确保 pos_embedding 被正确切片和扩展
        pos_emb = self.pos_embedding[:, :seq_len, :] # 切片到 seq_len
        if pos_emb.size(1) < seq_len:
             raise ValueError(f"序列长度 {seq_len} 超过最大序列长度 {self.pos_embedding.size(1)}")
        # 如果 batch_size > 1，则无需扩展，广播会处理它。如果 batch_size=1，则它已经是正确的。

        x = x + pos_emb # 添加位置嵌入

        x = self.encoder(x)   # (batch_size, seq_len, hidden_dim)
        decoded = self.decoder(x)
        return decoded

# --- 模型和缩放器加载 ---
DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu' # 检查是否有可用的 CUDA 设备
MODEL_PATH = os.path.join(os.path.dirname(__file__), "transformer_autoencoder_custom.pth")      # 模型文件路径
SCALER_PATH = os.path.join(os.path.dirname(__file__), "scaler.pkl")                             # 缩放器文件路径

def load_trained_model(model_path, device='cpu'):
    """加载预训练的 TransformerAutoencoder 模型，并处理 pos_embedding 形状不匹配的问题。"""
    input_dim = 12      # 输入维度
    hidden_dim = 64     # 隐藏层维度
    n_layers = 3        # Transformer 层数
    n_heads = 4         # 多头注意力头数
    dropout = 0.1       # Dropout 比例
    dim_feedforward = 256 # 前馈网络维度
    init_alpha = 0.01   # DyT 初始化 alpha 值
    max_seq_len = 256   # 最大序列长度，确保与训练设置匹配

    # 先实例化模型结构
    model = TransformerAutoencoder(
        input_dim, hidden_dim, n_layers, n_heads,
        dropout=dropout, dim_feedforward=dim_feedforward,
        init_alpha=init_alpha, max_seq_len=max_seq_len
    )
    try:
        # 加载权重字典
        # 如果适用，为了安全起见，使用 weights_only=True
        state_dict = torch.load(model_path, map_location=torch.device(device), weights_only=True)

        # --- 新增：检查并调整 pos_embedding 的形状 ---
        param_key = 'pos_embedding'
        if param_key in state_dict:
            loaded_param = state_dict[param_key]
            expected_shape = (1, max_seq_len, hidden_dim) # 当前模型期望的形状
            original_shape = (max_seq_len, hidden_dim)   # Checkpoint 中可能的原始形状

            # 检查加载的参数形状是否是旧的形状 [256, 64]
            if loaded_param.dim() == 2 and loaded_param.shape == original_shape:
                 print(f"检测到 '{param_key}' 的形状为 {loaded_param.shape}，"
                       f"正在调整为模型期望的形状 {expected_shape}。")
                 # 在第 0 维增加一个维度 (batch dimension)
                 state_dict[param_key] = loaded_param.unsqueeze(0)
            # 检查加载的参数形状是否已经是期望的形状 [1, 256, 64]
            elif loaded_param.shape == expected_shape:
                 print(f"'{param_key}' 在 Checkpoint 中的形状 {loaded_param.shape} 与模型定义匹配。")
            # 处理其他意外情况
            else:
                 print(f"警告：'{param_key}' 在 Checkpoint 中的形状 {loaded_param.shape} "
                       f"与期望的形状 {expected_shape} 或可能的原始形状 {original_shape} 都不符。")
                 # 这里可以选择抛出错误或尝试继续，但可能会失败
                 # raise ValueError(f"无法处理的 '{param_key}' 形状: {loaded_param.shape}")
        else:
             print(f"警告：在加载的 state_dict 中未找到参数 '{param_key}'。")
        # --- 调整结束 ---

        # 加载（可能已调整过的）权重字典到模型
        model.load_state_dict(state_dict)
        print(f"模型状态字典从 {model_path} 加载成功。")

    except FileNotFoundError:
        print(f"错误：在 {model_path} 未找到模型文件。无法启动应用程序。")
        raise # 抛出异常
    except Exception as e:
        # 捕获加载 state_dict 过程中的其他错误
        print(f"加载模型 state_dict 时出错: {e}")
        raise # 记录后重新抛出异常

    model.to(device)
    model.eval() # 将模型设置为评估模式
    return model

def load_or_fit_scaler(scaler_path, fit_dim=12):
    """加载缩放器，如果找不到则使用随机数据拟合一个新的缩放器。"""
    scaler = StandardScaler()
    if os.path.exists(scaler_path):
        try:
            with open(scaler_path, "rb") as f:
                scaler_data = pickle.load(f)
            # 在访问之前确保键存在
            if 'mean' in scaler_data and 'std' in scaler_data:
                 scaler.mean = np.array(scaler_data['mean'])
                 scaler.std = np.array(scaler_data['std'])
                 print(f"缩放器从 {scaler_path} 加载成功。")
            else:
                 print("找到缩放器文件，但缺少 'mean' 或 'std' 键。使用随机数据进行拟合。")
                 example_for_fit = np.random.randn(100, fit_dim) # 使用 fit_dim
                 scaler.fit(example_for_fit)
                 # （可选）保存新拟合的缩放器
                 # with open(scaler_path, "wb") as f:
                 #     pickle.dump({'mean': scaler.mean.tolist(), 'std': scaler.std.tolist()}, f)
        except Exception as e:
            print(f"从 {scaler_path} 加载缩放器时出错: {e}。使用随机数据进行拟合。")
            example_for_fit = np.random.randn(100, fit_dim) # 使用 fit_dim
            scaler.fit(example_for_fit)
    else:
        print(f"在 {scaler_path} 未找到缩放器文件。使用随机数据进行拟合。")
        example_for_fit = np.random.randn(100, fit_dim) # 使用 fit_dim
        scaler.fit(example_for_fit)
        # （可选）保存新拟合的缩放器
        # with open(scaler_path, "wb") as f:
        #     pickle.dump({'mean': scaler.mean.tolist(), 'std': scaler.std.tolist()}, f)
    return scaler

# --- 全局变量 (在启动时加载模型和缩放器) ---
try:
    model = load_trained_model(MODEL_PATH, device=DEVICE)
    # 传递预期的特征维度
    scaler = load_or_fit_scaler(SCALER_PATH, fit_dim=12)
except Exception as e:
    # 如果模型加载失败，退出或进行适当处理
    print(f"启动期间发生严重错误: {e}")
    exit() # 如果无法加载模型则退出

# --- 异常检测函数 ---
def detect_anomaly(record: Record, model: TransformerAutoencoder, scaler: StandardScaler, threshold=0.1, device='cpu'):
    """
    对单个记录执行异常检测。

    Args:
        record (Record): 作为 Pydantic 模型的输入数据。
        model (TransformerAutoencoder): 加载的 PyTorch 模型。
        scaler (StandardScaler): 拟合的缩放器对象。
        threshold (float): 用于异常检测的 MSE 阈值。
        device (str): 运行推理的设备 ('cpu' 或 'cuda')。

    Returns:
        tuple: 包含以下内容的元组：
            - str: 车辆 ID。
            - bool: 如果检测到异常则为 True，否则为 False。
            - float: 计算出的 MSE 损失。
    """
    features, vehicle_id = parse_record_from_model(record)
    features = np.array(features).reshape(1, -1) # 为缩放器重塑形状

    # 在转换之前确保缩放器已拟合
    if scaler.mean is None or scaler.std is None:
        raise RuntimeError("缩放器未拟合。无法执行转换。")

    try:
        # transform 期望 2D 数组
        features_std = scaler.transform(features)
    except Exception as e:
        print(f"缩放期间出错: {e}")
        raise HTTPException(status_code=500, detail="数据缩放期间出错。")

    # 为模型重塑形状: (batch_size, seq_len, input_dim) -> (1, 1, 12)
    # 假设单个记录预测的 seq_len 为 1
    # 添加 seq_len 维度
    input_tensor = torch.tensor(features_std, dtype=torch.float32, device=device).unsqueeze(1)

    # 调试: 在模型推理前打印张量形状
    # print(f"输入张量形状: {input_tensor.shape}") # 应为 [1, 1, 12]

    with torch.no_grad(): # 推理时禁用梯度计算
        try:
            reconstructed = model(input_tensor)
            # 调试: 打印重构张量的形状
            # print(f"重构张量形状: {reconstructed.shape}") # 应与输入形状匹配

            # 确保形状匹配以进行 MSE 计算
            if reconstructed.shape != input_tensor.shape:
                 raise ValueError(f"形状不匹配: 输入 {input_tensor.shape}, 重构 {reconstructed.shape}")

            mse_loss = torch.mean((reconstructed - input_tensor) ** 2).item()

        except Exception as e:
            print(f"模型推理或损失计算期间出错: {e}")
            # 根据潜在的模型问题考虑更具体的错误处理
            raise HTTPException(status_code=500, detail=f"模型推理错误: {e}")

    print(f'车辆: {vehicle_id} 的 MSE: {mse_loss:.4f}') # 记录 MSE
    is_anomaly = mse_loss > threshold
    return vehicle_id, is_anomaly, mse_loss

# --- FastAPI 应用 ---
app = FastAPI(
    title="车辆异常检测 API",
    description="接收车辆数据记录，并使用 Transformer 自编码器检测异常。",
    version="1.0.1" # 版本号递增
)

# 添加CORS中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/detect-anomaly/",
          response_model=Dict[str, str], # 定义响应模型
          summary="检测异常",
          tags=["异常检测"])
async def detect(record: Record):
    """
    接收车辆数据，执行异常检测，并返回状态。

    - **record**: 包含车辆 'header' 和 'body' 数据的 JSON 对象。

    返回:
    - 包含 'message' 的 JSON 对象，指示车辆 ID 和状态 (正常/异常)。
    """
    try:
        vehicle_id, anomaly, mse = detect_anomaly(record, model, scaler, threshold=0.1, device=DEVICE)
        status = "异常" if anomaly else "正常" # 异常/正常
        return {"message": f"车辆编号: {vehicle_id}, 状态: {status}, mse: {mse}"}
    except HTTPException as http_exc:
        # 直接重新抛出 HTTPException
        raise http_exc
    except RuntimeError as r_err:
        # 处理特定的运行时错误，如未拟合的缩放器
        raise HTTPException(status_code=500, detail=f"内部服务器错误: {r_err}")
    except ValueError as v_err:
        # 处理值错误，如形状不匹配
         raise HTTPException(status_code=500, detail=f"数据处理错误: {v_err}")
    except Exception as e:
        # 捕获检测期间的任何其他意外错误
        print(f"/detect-anomaly 端点出现意外错误: {e}") # 记录错误
        raise HTTPException(status_code=500, detail=f"发生意外错误: {e}")

@app.get("/", include_in_schema=False) # 在 API 文档中不显示此路径
async def root():
    return {"message": "欢迎使用异常检测 API。将数据 POST 到 /detect-anomaly/"}

# --- 主执行块 ---
if __name__ == "__main__":
    print(f"在 0.0.0.0:8081 上启动服务器，使用设备: {DEVICE}")
    # 使用 uvicorn 运行 FastAPI 服务器
    # 在开发中使用 reload=True 以在代码更改时自动重启
    uvicorn.run("main:app", host="0.0.0.0", port=8081, reload=False)
    # 对于生产环境，考虑使用更多工作进程，例如：
    # uvicorn.run("main:app", host="0.0.0.0", port=8081, workers=4)

