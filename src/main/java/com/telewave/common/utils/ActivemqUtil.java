package com.telewave.common.utils;

import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageNotWriteableException;
import javax.jms.TextMessage;

/**
 * Created by peiyang on 2018/3/21.
 */
public class ActivemqUtil {
    /**
     * 获取纯文本信息。
     *
     * @param message 表示消息的Message对象。
     * @return
     * @throws JMSException
     * @throws ClassCastException
     */
    public static String getTextMessage(Message message) throws JMSException, ClassCastException {
        if (message == null) {
            return null;
        }
        TextMessage text = (TextMessage) message;
        if (text == null) {
            throw new ClassCastException("Message message connot cast to TextMessage.");
        }
        return text.getText();
    }

    /**
     * 获取表示消息的Message对象。
     *
     * @param text 纯文本消息内容。
     * @return
     * @throws MessageNotWriteableException
     */
    public static Message getMessage(String text) throws MessageNotWriteableException {
        if (text == null) {
            return null;
        }

        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText(text);

        return activeMQTextMessage;
    }
}
