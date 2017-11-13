package com.ehu.pay.weixin;

import com.ehu.pay.exception.PayException;
import com.ehu.pay.weixin.entity.WeChatRefundInfo;
import com.ehu.pay.weixin.entity.WeChatResponseVO;
import com.ehu.pay.weixin.entity.WeChatpayOrder;
import com.ehu.pay.weixin.entity.WechatBusinessPay;
import com.ehu.pay.weixin.util.Signature;
import com.ehu.pay.weixin.weixinpay.*;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;

/**
 * @author AlanSun
 */
@Slf4j
public class WeChatPayUtil {

    /**
     * 微信支付(app支付与jsapi共用)
     *
     * @param tradeType 1与3 1:app支付；3：jsapi支付 4:小程序
     * @throws PayException
     */
    public static WeChatResponseVO createWeiXinPackage(WeChatpayOrder order, int tradeType) throws PayException {
        if (1 == tradeType || 3 == tradeType) {
            return WeChatPayGetPrepay.gerneratorPrepay(order, tradeType);
        } else if (4 == tradeType) {
            return WeChatPayGetPrepay.gerneratorPrepayXcx(order);
        }
        return null;
    }

    /**
     * 获取扫码支付二维码
     *
     * @param order
     * @throws PayException
     */
    public static String getScanPayInfo(WeChatpayOrder order) throws PayException {
        return WeChatPayGetPrepay.gerneratorPrepayScan(order);
    }

    /**
     * 微信订单查询
     *
     * @throws PayException
     */
    public static Object queryWeChatOrder(String transaction_id, String queryFlag) throws PayException {
        return GetWeChatQuerySign.getQuertResult(transaction_id, queryFlag);
    }

    /**
     * 检验API返回的数据里面的签名是否合法，避免数据在传输的过程中被第三方篡改
     *
     * @param map API返回的XML数据字符串
     * @return API签名是否合法
     * @throws JDOMException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws SAXException
     */
    public static boolean checkIsSignValidFromResponseString(Map<String, String> map) throws JDOMException, IOException, IllegalAccessException {
        String signFromAPIResponse = map.get("sign").toString();
        if (signFromAPIResponse == "" || signFromAPIResponse == null) {
            log.error("API返回的数据签名数据不存在，有可能被第三方篡改!!!");
            return false;
        }
        log.info("服务器回包里面的签名是:" + signFromAPIResponse);
        //清掉返回数据对象里面的Sign数据（不能把这个数据也加进去进行签名），然后用签名算法进行签名
        map.put("sign", "");
        //将API返回的数据根据用签名算法进行计算新的签名，用来跟API返回的签名进行比较
        String signForAPIResponse = Signature.getSign(map);
        log.info("生成的签名是:" + signForAPIResponse);
        if (!signForAPIResponse.equals(signFromAPIResponse)) {
            //签名验不过，表示这个API返回的数据有可能已经被篡改了
            log.error("API返回的数据签名验证不通过，有可能被第三方篡改!!!");
            return false;
        }
        return true;
    }

    /**
     * 微信退款
     *
     * @return boolean
     * @throws PayException
     */
    public static boolean weChatRefund(WeChatRefundInfo weChatRefundInfo) throws PayException {
        return WeChatRefund.weChatRefundOper(weChatRefundInfo);
    }

    /**
     * 小程序微信退款
     *
     * @return boolean
     * @throws PayException
     */
    public static boolean weChatRefundXcx(WeChatRefundInfo weChatRefundInfo) throws PayException {
        return WeChatRefund.weChatRefundOperXcx(weChatRefundInfo);
    }

    /**
     * 微信企业转账
     *
     * @param wechatBusinessPay
     * @return boolean
     * @throws PayException
     */
    public static boolean weChatBusinessPayForUser(WechatBusinessPay wechatBusinessPay) throws PayException {
        return WechatBusinessPayForUser.weChatPayBusinessPayforUser(wechatBusinessPay);
    }

    /**
     * 下载账单
     *
     * @param time
     * @throws PayException
     */
    public static void downloadBill(String time, String desPath) throws PayException {
        DownloadBill.downloadBill(time, desPath);
    }
}