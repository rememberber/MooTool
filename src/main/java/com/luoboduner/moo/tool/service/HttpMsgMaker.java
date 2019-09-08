package com.luoboduner.moo.tool.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.luoboduner.moo.tool.bean.HttpMsg;
import com.luoboduner.moo.tool.ui.form.fun.HttpRequestForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;

import javax.swing.table.DefaultTableModel;
import java.net.HttpCookie;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

/**
 * <pre>
 * http消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/16.
 */
@Slf4j
public class HttpMsgMaker {

    public static String method;
    public static String url;
    public static String body;
    public static String bodyType;
    public static List<HttpRequestForm.NameValueObject> paramList;
    public static List<HttpRequestForm.NameValueObject> headerList;
    public static List<HttpRequestForm.CookieObject> cookieList;

    public static void prepare() {
        method = (String) HttpRequestForm.getInstance().getMethodComboBox().getSelectedItem();
        url = HttpRequestForm.getInstance().getUrlTextField().getText().trim();
        body = HttpRequestForm.getInstance().getBodyTextArea().getText();
        bodyType = (String) HttpRequestForm.getInstance().getBodyTypeComboBox().getSelectedItem();

        // Params=========================
        if (HttpRequestForm.getInstance().getParamTable().getModel().getRowCount() == 0) {
            HttpRequestForm.initParamTable();
        }
        DefaultTableModel paramTableModel = (DefaultTableModel) HttpRequestForm.getInstance().getParamTable().getModel();
        int rowCount = paramTableModel.getRowCount();
        HttpRequestForm.NameValueObject nameValueObject;
        paramList = Lists.newArrayList();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) paramTableModel.getValueAt(i, 0)).trim();
            String value = ((String) paramTableModel.getValueAt(i, 1)).trim();
            nameValueObject = new HttpRequestForm.NameValueObject();
            nameValueObject.setName(name);
            nameValueObject.setValue(value);
            paramList.add(nameValueObject);
        }
        // Headers=========================
        if (HttpRequestForm.getInstance().getHeaderTable().getModel().getRowCount() == 0) {
            HttpRequestForm.initHeaderTable();
        }
        DefaultTableModel headerTableModel = (DefaultTableModel) HttpRequestForm.getInstance().getHeaderTable().getModel();
        rowCount = headerTableModel.getRowCount();
        headerList = Lists.newArrayList();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) headerTableModel.getValueAt(i, 0)).trim();
            String value = ((String) headerTableModel.getValueAt(i, 1)).trim();
            nameValueObject = new HttpRequestForm.NameValueObject();
            nameValueObject.setName(name);
            nameValueObject.setValue(value);
            headerList.add(nameValueObject);
        }
        // Cookies=========================
        if (HttpRequestForm.getInstance().getCookieTable().getModel().getRowCount() == 0) {
            HttpRequestForm.initCookieTable();
        }
        DefaultTableModel cookieTableModel = (DefaultTableModel) HttpRequestForm.getInstance().getCookieTable().getModel();
        rowCount = cookieTableModel.getRowCount();
        cookieList = Lists.newArrayList();
        HttpRequestForm.CookieObject cookieObject;
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) cookieTableModel.getValueAt(i, 0)).trim();
            String value = ((String) cookieTableModel.getValueAt(i, 1)).trim();
            String domain = ((String) cookieTableModel.getValueAt(i, 2)).trim();
            String path = ((String) cookieTableModel.getValueAt(i, 3)).trim();
            String expiry = ((String) cookieTableModel.getValueAt(i, 4)).trim();
            cookieObject = new HttpRequestForm.CookieObject();
            cookieObject.setName(name);
            cookieObject.setValue(value);
            cookieObject.setDomain(domain);
            cookieObject.setPath(path);
            cookieObject.setExpiry(expiry);
            cookieList.add(cookieObject);
        }
    }

    public HttpMsg makeMsg() {
        HttpMsg httpMsg = new HttpMsg();

        httpMsg.setUrl(url);
        httpMsg.setBody(body);

        HashMap<String, Object> paramMap = Maps.newHashMap();
        for (HttpRequestForm.NameValueObject nameValueObject : paramList) {
            paramMap.put(nameValueObject.getName(), nameValueObject.getValue());
        }
        httpMsg.setParamMap(paramMap);

        HashMap<String, Object> headerMap = Maps.newHashMap();
        for (HttpRequestForm.NameValueObject nameValueObject : headerList) {
            headerMap.put(nameValueObject.getName(), nameValueObject.getValue());
        }
        httpMsg.setHeaderMap(headerMap);

        List<HttpCookie> cookies = Lists.newArrayList();
        for (HttpRequestForm.CookieObject cookieObject : cookieList) {
            HttpCookie httpCookie = new HttpCookie(cookieObject.getName(), cookieObject.getValue());
            httpCookie.setDomain(cookieObject.getDomain());
            httpCookie.setPath(cookieObject.getPath());
            try {
                httpCookie.setMaxAge(DateUtils.parseDate(cookieObject.getExpiry(), "yyyy-MM-dd HH:mm:ss").getTime());
            } catch (ParseException e) {
                log.error(e.toString());
            }
            cookies.add(httpCookie);
        }
        httpMsg.setCookies(cookies);

        return httpMsg;
    }
}
