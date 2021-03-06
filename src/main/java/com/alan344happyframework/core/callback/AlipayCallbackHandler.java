package com.alan344happyframework.core.callback;

import com.alipay.api.internal.util.AlipaySignature;
import com.alan344happyframework.config.AliPay;
import com.alan344happyframework.constants.ErrorCode;
import com.alan344happyframework.constants.PayBaseConstants;
import com.alan344happyframework.core.ConcretePayService;
import com.alan344happyframework.exception.PayException;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author AlanSun
 * @date 2019/7/1 16:45
 **/
@Slf4j
public class AlipayCallbackHandler implements CallbackHandler {
    private AlipayCallbackHandler() {
    }

    private static AlipayCallbackHandler instance = new AlipayCallbackHandler();

    public static AlipayCallbackHandler getInstance() {
        return instance;
    }

    private static final AliPay aliPay = AliPay.getInstance();

    @Override
    public void handler(HttpServletRequest request, HttpServletResponse response, boolean isVerify, ConcretePayService concretePayService) {
        PrintWriter out = null;
        try {
            out = response.getWriter();
            Map<String, String> paramsMap = getAlipayCallBackMap(request);
            log.info("支付宝回调开始, 参数：{}", paramsMap.toString());

            //调用SDK验证签名
            if (isVerify && !AlipaySignature.rsaCheckV1(paramsMap, aliPay.getOpenPublicKey(), aliPay.getInputCharset(), paramsMap.get("sign_type)"))) {
                throw new PayException(ErrorCode.VERIFY_ERROR);
            }
            concretePayService.handler(this.getCallBackParam(paramsMap));
            out.println("success");
        } catch (Exception e) {
            assert out != null;
            out.println("fail");
            log.error("阿里扫码回调fail", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private CallBackParam getCallBackParam(Map<String, String> params) throws PayException {
        // 商户支付单号
        String out_trade_no = params.get("out_trade_no");
        // 支付宝交易号
        String trade_no = params.get("trade_no");
        // 交易状态
        String trade_status = params.get("trade_status");
        // 付款方支付宝账号
        String buyerId = params.get("buyer_id");
        String total_amount = null;
        if (params.containsKey("total_amount")) {
            total_amount = params.get("total_amount");
        }

        //验签成功则继续业务操作，最后在response中返回success
        if ("TRADE_SUCCESS".equals(trade_status)) {

            CallBackParam callBackParam = new CallBackParam();
            callBackParam.setOrderId(out_trade_no);
            callBackParam.setCallBackPrice(total_amount);
            callBackParam.setPayAccount(buyerId);
            callBackParam.setPayType(PayBaseConstants.ORDER_PAY_TYPE_ALIPAY);
            callBackParam.setThirdOrderId(trade_no);

            return callBackParam;
        } else {
            throw new PayException("trade_status error");
        }
    }

    /**
     * 处理支付宝返回参数
     *
     * @param request HttpServletRequest
     * @return paramsMap
     */
    private static Map<String, String> getAlipayCallBackMap(HttpServletRequest request) {
        //获取支付宝POST过来反馈信息
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> paramsMap = new HashMap<>(requestParams.size());
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
            paramsMap.put(name, valueStr);
        }
        return paramsMap;
    }
}
