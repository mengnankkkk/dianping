package com.mengnankk.service.Impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.mengnankk.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AliyunSmsService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Client aliyunSmsClient;

    @Value("${sms.code.expiration-seconds}")
    private long smsCodeExpirationSeconds;

    @Value("${aliyun.sms.sign-name}")
    private String signName;

    @Value("${aliyun.sms.template-code-register}")
    private String registerTemplateCode;

    @Value("${aliyun.sms.template-code-reset-password}")
    private String resetPasswordTemplateCode;


    /**
     * 发送验证码-注册
     * @param phoneNumber
     */
    public void sendRegisterSmsCode(String phoneNumber) {
        String code = generateRandomCode(6);
        String redisKey = "sms:code:register:" + phoneNumber;
        redisTemplate.opsForValue().set(redisKey, code, smsCodeExpirationSeconds, TimeUnit.SECONDS);
        log.info("Generated register SMS code for {}: {}", phoneNumber, code);

        sendSms(phoneNumber, registerTemplateCode, "{\"code\":\"" + code + "\"}");
    }
    public void sendResetPasswordSmsCode(String phoneNumber){
        String code = generateRandomCode(6);
        String redisKey = "sms:code:reset_password:" + phoneNumber;
        redisTemplate.opsForValue().set(redisKey,code,smsCodeExpirationSeconds,TimeUnit.SECONDS);
        log.info("Generated reset password SMS code for {}: {}", phoneNumber, code);

        sendSms(phoneNumber, resetPasswordTemplateCode, "{\"code\":\"" + code + "\"}");
    }

    /**
     * 发送验证码
     * @param phoneNumber
     * @param templateCode
     * @param templateParam
     */
    private void sendSms(String phoneNumber, String templateCode, String templateParam){
        SendSmsRequest sendSmsRequest = new SendSmsRequest()
                .setPhoneNumbers(phoneNumber)
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setTemplateParam(templateParam);
        RuntimeOptions runtime = new RuntimeOptions();
        try {
            SendSmsResponse response = aliyunSmsClient.sendSmsWithOptions(sendSmsRequest,runtime);
            if (!"OK".equals(response.getBody().getCode())){
                log.error("Failed to send SMS to {}. ErrorCode: {}, Message: {}",
                        phoneNumber, response.getBody().getCode(), response.getBody().getMessage());
                throw new AuthException("短信发送失败: " + response.getBody().getMessage());
            }
            log.info("SMS sent successfully to {}. RequestId: {}", phoneNumber, response.getBody().getRequestId());
        } catch (Exception e) {
            log.error("Error sending SMS to {}: {}", phoneNumber, e.getMessage(), e);
            throw new AuthException("短信发送异常，请稍后再试。");
        }
    }

    /**
     * 校验验证码-找回密码使用
     * @param phoneNumber
     * @param code
     * @return
     */
    public boolean verifyRegisterSmsCode(String phoneNumber, String code){
        String redisKey = "sms:code:register:" + phoneNumber;
        return verifySmsCode(redisKey,code);
    }

    /**
     * 生成验证码
     * @param length
     * @return
     */
    private String generateRandomCode(int length) {
            Random random = new Random();
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < length; i++) {
                code.append(random.nextInt(10));
            }
            return code.toString();
    }

    /**
     * 验证码发送校验
     * @param redisKey
     * @param code
     * @return
     */
    private boolean verifySmsCode(String redisKey, String code) {
            String storedCode = (String) redisTemplate.opsForValue().get(redisKey);

            if (storedCode == null) {
                throw new AuthException("验证码已过期或未发送，请重新获取。");
            }
            if (!storedCode.equals(code)) {
                throw new AuthException("验证码不正确。");
            }
            // 验证成功后删除Redis中的验证码，防止重复使用
            redisTemplate.delete(redisKey);
            return true;
    }
}
