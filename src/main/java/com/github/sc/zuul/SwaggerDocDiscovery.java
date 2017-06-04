package com.github.sc.zuul;

import com.github.sc.dao.DocumentDao;
import com.github.sc.model.Document;
import com.netflix.config.ConfigurationManager;
import io.swagger.models.Swagger;
import io.swagger.parser.Swagger20Parser;
import org.h2.util.NetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuyu on 2017/5/10.
 */
@Component
public class SwaggerDocDiscovery implements DiscoveryClient {

    @Autowired
    protected ServerProperties serverProperties;

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private DocumentDao documentDao;

    @Override
    public String description() {
        return "Swagger client proxy";
    }

    @Override
    public ServiceInstance getLocalServiceInstance() {
        return new DefaultServiceInstance(applicationName, NetUtils.getLocalAddress(), serverProperties.getPort(), serverProperties.getSsl() != null);
    }

    @Override
    public List<ServiceInstance> getInstances(String s) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        try {
            Document one = documentDao.findOne(Integer.parseInt(s));
            URL url = getUrl(one);
            DefaultServiceInstance instance = new DefaultServiceInstance("" + one.getId(), url.getHost(), url.getPort(), url.getHost().contains("https://"));
            serviceInstances.add(instance);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serviceInstances;
    }

    @Override
    public List<String> getServices() {
        List<String> services = new ArrayList<>();
        for (Document document : documentDao.findAll()) {
            services.add("" + document.getId());
            String host = getHost(document);
            ConfigurationManager.getConfigInstance()
                    .addProperty(document.getId() + ".ribbon.listOfServers", host);
        }

        return services;
    }


    protected URL getUrl(Document document) throws MalformedURLException {
        String host = "";
        try {
            Swagger swagger = new Swagger20Parser()
                    .parse(document.getContent());
            String scheme = "http://";
            if (swagger.getSchemes() != null && swagger.getSchemes().size() > 0) {
                scheme = swagger.getSchemes().get(0).toValue()+"://";
            }

            host = scheme + swagger.getHost();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new URL(host);
    }

    protected String getHost(Document document) {
        try {
            URL url = getUrl(document);
            return url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "http://";
    }

}
