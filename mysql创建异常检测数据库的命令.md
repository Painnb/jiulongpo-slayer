CREATE TABLE acceleration_exp (
    id INT AUTO_INCREMENT PRIMARY KEY,        -- 自增id
    vehicleId VARCHAR(255) NOT NULL,           -- 车辆id
    accelerationLon DOUBLE NOT NULL,          -- 纵向加速度
    accelerationLat DOUBLE NOT NULL,          -- 横向加速度
    accelerationVer DOUBLE NOT NULL,          -- 垂向加速度
    timestamp DATETIME NOT NULL               -- 时间戳
);



CREATE TABLE speed_exp (
    id INT AUTO_INCREMENT PRIMARY KEY,        -- 自增id
    vehicleId VARCHAR(255) NOT NULL,           -- 车辆id
    velocityGNSS DOUBLE NOT NULL,             -- GNSS速度
    velocityCAN DOUBLE NOT NULL,              -- 当前车速
    timestamp DATETIME NOT NULL               -- 时间戳
);



CREATE TABLE engine_exp (
    id INT AUTO_INCREMENT PRIMARY KEY,        -- 自增id
    vehicleId VARCHAR(255) NOT NULL,           -- 车辆id
    engineSpeed INT NOT NULL,                 -- 发动机转速
    engineTorque INT NOT NULL,                -- 发动机扭矩
    timestamp DATETIME NOT NULL               -- 时间戳
);



CREATE TABLE brake_exp (
    id INT AUTO_INCREMENT PRIMARY KEY,        -- 自增id
    vehicleId VARCHAR(255) NOT NULL,           -- 车辆id
    brakeFlag BOOLEAN NOT NULL,                -- 制动踏板开关
    brakePos INT NOT NULL,                     -- 制动踏板开度
    brakePressure INT NOT NULL,                -- 制动主缸压力
    timestamp DATETIME NOT NULL               -- 时间戳
);

CREATE TABLE steering_exp (
    id INT AUTO_INCREMENT PRIMARY KEY,        -- 自增id
    vehicleId VARCHAR(255) NOT NULL,           -- 车辆id
    steeringAngle INT NOT NULL,                -- 方向盘转角
    yawRate INT NOT NULL,                      -- 横摆角速度
    timestamp DATETIME NOT NULL               -- 时间戳
);



CREATE TABLE timestamp_exp (
    id INT AUTO_INCREMENT PRIMARY KEY,        -- 自增id
    vehicleId VARCHAR(255) NOT NULL,           -- 车辆id
    timestampGNSS DATETIME NOT NULL,           -- GNSS时间戳
    timestamp3 DATETIME NOT NULL,              -- 文档没写这是什么
    timestamp4 DATETIME NOT NULL,              -- 文档没写这是什么
    timestamp DATETIME NOT NULL               -- 时间戳
);



CREATE TABLE geo_location_exp (
    id INT AUTO_INCREMENT PRIMARY KEY,        -- 自增id
    vehicleId VARCHAR(255) NOT NULL,           -- 车辆id
    longitude DOUBLE NOT NULL,                 -- 经度
    latitude DOUBLE NOT NULL,                  -- 纬度
    timestamp DATETIME NOT NULL               -- 时间戳
);