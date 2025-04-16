// package org.swu.vehiclecloud.controller;

// import com.alipay.api.AlipayApiException;
// import com.alipay.api.AlipayClient;
// import com.alipay.api.DefaultAlipayClient;
// import com.alipay.api.request.AlipayTradePagePayRequest;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.web.bind.annotation.*;
// import org.swu.vehiclecloud.controller.template.ApiResult;

// import javax.servlet.http.HttpServletRequest;
// import java.util.HashMap;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/alipay")
// public class AlipayController {

//     @Value("${alipay.appId}")
//     private String appId;
//     @Value("${alipay.privateKey}")
//     private String privateKey;
//     @Value("${alipay.publicKey}")
//     private String publicKey;
//     @Value("${alipay.serverUrl}")
//     private String serverUrl;
//     @Value("${alipay.returnUrl}")
//     private String returnUrl;
//     @Value("${alipay.notifyUrl}")
//     private String notifyUrl;

//     /**
//      * 创建支付宝支付订单
//      */
//     @PostMapping("/create")
//     public ApiResult<String> createOrder(@RequestParam String orderNo, 
//                                         @RequestParam String amount, 
//                                         @RequestParam String subject) throws AlipayApiException {
//         AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, appId, privateKey, "json", "UTF-8", publicKey, "RSA2");
//         AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
//         alipayRequest.setReturnUrl(returnUrl);
//         alipayRequest.setNotifyUrl(notifyUrl);

//         Map<String, String> bizContent = new HashMap<>();
//         bizContent.put("out_trade_no", orderNo);
//         bizContent.put("total_amount", amount);
//         bizContent.put("subject", subject);
//         bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

//         alipayRequest.setBizContent(org.json.JSONObject.valueToString(bizContent));
//         String form = alipayClient.pageExecute(alipayRequest).getBody();
//         return ApiResult.of(200, "OK", form);
//     }

//     /**
//      * 支付宝异步通知处理
//      */
//     @PostMapping("/notify")
//     public String notify(HttpServletRequest request) {
//         // 处理支付宝异步通知
//         return "success";
//     }

//     /**
//      * 查询支付状态
//      */
//     @GetMapping("/query")
//     public ApiResult<Map<String, String>> queryOrder(@RequestParam String orderNo) {
//         // 查询订单状态逻辑
//         return ApiResult.of(200, "OK", new HashMap<>());
//     }
// }