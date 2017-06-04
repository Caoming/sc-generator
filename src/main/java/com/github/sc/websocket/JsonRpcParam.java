package com.github.sc.websocket;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

public class JsonRpcParam implements Serializable {
    private static final long serialVersionUID = 1064223171940612201L;

    //兼容 jsonrpc 如果携带次参数 将以jsonrpc 格式返回
    private String jsonrpc = "2.0";

    //兼容 jsonrpc
    private String id;

    //参数
    private JsonNode params;

    //参数类型
    private String[] paramsType;

    public JsonRpcParam() {
    }


    public String getJsonrpc() {
        return jsonrpc;
    }

    public JsonRpcParam setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
        return this;
    }

    public String getId() {
        return id;
    }

    public JsonRpcParam setId(String id) {
        this.id = id;
        return this;
    }

    public JsonRpcParam setParams(JsonNode params) {
        this.params = params;
        return this;
    }

    public JsonNode getParams() {
        return params;
    }

    public String[] getParamsType() {
        return paramsType;
    }

    public JsonRpcParam setParamsType(String[] paramsType) {
        this.paramsType = paramsType;
        return this;
    }
}