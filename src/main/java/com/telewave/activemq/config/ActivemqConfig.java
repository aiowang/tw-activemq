package com.telewave.activemq.config;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.telewave.activemq.annotations.ConsumerListener;
import com.telewave.activemq.interfaces.Consumer;
import com.telewave.common.holder.SpringContextHolder;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.transport.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;

import javax.jms.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableJms
public class ActivemqConfig implements TransportListener {

    //region 私有变量
    private static final Logger logger = LoggerFactory.getLogger(ActivemqConfig.class);

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user}")
    private String user;

    @Value("${spring.activemq.password}")
    private String password;

/*    @Value("${app.ip}")
    private String appIp;*/

    @Value("${app.identity}")
    private String appIdentity;

    @Value("${app.type}")
    private int appType;

    @Value("${app.name}")
    private String appName;
    //endregion


    //region 公共方法

    /**
     * 注册ActiveMQ连接工厂
     *
     * @return
     */
    @Primary
    @Bean(name = "activemqConnectionFactory")
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        try {
            //中文编码转换
            byte[] temp = appName.getBytes("iso-8859-1");
            appName = new String(temp, 0, temp.length, "utf-8");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        String appIp = "localhost";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            appIp = addr.getHostAddress();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }

        appName = appName + "_" + ("localhost".equals(appIp) ? "localhost" : appIp.split("\\.")[3]);

        StringBuffer clientId = new StringBuffer();
        clientId.append("{\"ip\":\"" + appIp + "\",");
        clientId.append("\"identity\":\"" + appIdentity + "\",");
        clientId.append("\"type\":" + appType + ",");
        clientId.append("\"name\":\"" + appName + "\"}");
        factory.setClientID(clientId.toString());
        factory.setTransportListener(this);
        return factory;
    }

    /**
     * 注册ActiveMQ的连接对象Connection
     *
     * @param activeMQConnectionFactory
     * @return
     */
    @Bean(name = "activemqConnection")
    public ActiveMQConnection activeMQConnection(ActiveMQConnectionFactory activeMQConnectionFactory) {
        ActiveMQConnection connection = null;
        try {
            connection = (ActiveMQConnection) activeMQConnectionFactory.createConnection();
            connection.start();
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * 注册ActiveMQ的Session
     *
     * @param connection
     * @return
     */
    @Bean(name = "activemqSession")
    public Session session(ActiveMQConnection connection) {
        Session session = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
        return session;
    }

    /**
     * 注册ActiveMQ的生产者。生产者一个应用只需一个即可。
     *
     * @param session
     * @return
     */
    @Bean(name = "activemqProducer")
    public MessageProducer messageProducer(Session session) {
        MessageProducer producer = null;
        try {
            producer = session.createProducer(null);
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
        return producer;
    }

    /**
     * 注册ActiveMQ的消费者
     *
     * @param session
     * @return
     */
    @Bean(name = "activemqConsumer")
    public Consumer consumer(Session session) {
        Consumer consumer = null;
        try {
            //创建所有消费者
            List<Consumer> customers = getConsumers();
            for (Consumer customer : customers) {
                //获取消费者中的所有方法
                Method methods[] = customer.getClass().getMethods();
                for (final Method method : methods) {
                    final ConsumerListener listener = method.getAnnotation(ConsumerListener.class);
                    MessageConsumer mc = null;

                    //给有监听注解的方法创建实际消费者
                    if (listener != null) {
                        if (listener.type() == ConsumerListener.QUEUE && !Strings.isNullOrEmpty(
                                listener.destination())) {
                            mc = session.createConsumer(session.createQueue(listener.destination()));
                        }
                        else if (listener.type() == ConsumerListener.TOPIC && !Strings.isNullOrEmpty(
                                listener.destination())) {
                            mc = session.createConsumer(session.createTopic(listener.destination()));
                        }
                    }

                    //添加消息监听
                    if (mc != null) {
                        mc.setMessageListener(new MessageListener() {
                            @Override
                            public void onMessage(Message message) {
                                try {
                                    method.invoke(customer, message);
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
        return consumer;
    }
    //endregion


    //region 公共方法，TransportListener成员
    @Override
    public void onCommand(Object command) {

    }

    @Override
    public void onException(IOException error) {

    }

    @Override
    public void transportInterupted() {
        logger.info("ActiveMQ连接断开...");
    }

    @Override
    public void transportResumed() {
        logger.info("ActiveMQ连接成功...");
    }
    //endregion


    //region 私有方法

    /**
     * 临时创建，可使用注解方式扫描添加
     */
    public List<Consumer> getConsumers() {
        List<Consumer> list = Lists.newArrayList();
        Map map = SpringContextHolder.getBeanWithAnnotation(ConsumerListener.class);//获取带有ConsumerListener注解的类
        Set<String> keys = map.keySet();
        for (String key : keys) {
            Class<? extends Object>[] ifs = map.get(key).getClass().getInterfaces();
            for (Class<? extends Object> i : ifs) {
                if (i.getName().equals(Consumer.class.getName())) {
                    list.add((Consumer) map.get(key));
                    break;
                }
            }
        }
        return list;
    }
}