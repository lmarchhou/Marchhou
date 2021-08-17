package com.uih.uplus.solar.shared.utils;

import com.alibaba.fastjson.JSONObject;
import com.uih.uplus.solar.shared.Model.ResponseModel;
import com.uih.uplus.solar.shared.Model.UserData;
import com.uih.uplus.solar.shared.Model.UserIdsModel;
import com.uih.uplus.solar.shared.Model.UserModel;
import com.uih.uplus.solar.shared.constant.LogCst;
import com.uih.uplus.solar.shared.constant.RedisConst;
import com.uih.uplus.solar.shared.constant.TokenCst;
import com.uih.uplus.solar.shared.constant.UrlVersionConst;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import lombok.Data;
import lombok.extern.uih.log.UIHLog;

/**
 * @ClassName: UserUtils
 * @Description: 获取用户相关信息工具类
 * @Author: liang.yan@united-imging.com
 * @date: 2020/3/18
 */
@UIHLog
@Component
@Data
@ConditionalOnBean(name = {"tokenUtil"})
public class UserUtils {
    private static final String MSG_CODE="msgCode";
    private static final String SUCCESS_ID="success.id";
    private static final String DATA="data";
    private static final String U_000000 = "U000000";

    @Value("${api.uapApiUrl:http}")
    private String uapApiUrl;
    private static final String USER_TYPE = UrlVersionConst.PORTAL_API_V_1 + "/user/co-type";
    private static final String USER_DETAILS_STR = UrlVersionConst.PORTAL_API_V_1 + "/user/details?";

    //客戶端凭据权限


    private static final String SUBSCRIPTION_TIME = UrlVersionConst.PORTAL_API_V_1 + "/tenant/sub-app/time";


    private static final String CURRENT_ROLE_NAME = UrlVersionConst.PORTAL_API_V_1 + "/user/current-role-name/page";

    private static final String USER_SOP_ID = UrlVersionConst.PORTAL_API_V_1 + "/user/sopid?";

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    private static final String USER_ID_NAME = UrlVersionConst.PORTAL_API_V_1 + "/user/id/name?";


    /**
     * @Description: 根据用户姓名获取用户列表
     * @parameter: appId
     * @parameter: roleCode
     * @parameter: baseUrl
     * @return: java.util.List<com.uih.uplus.solar.shared.Model.UserData>
     */
    public List<UserData> getUserDataListByName(String name) {
        String url;
        if (StringUtils.isEmpty(name)) {
            url = uapApiUrl + USER_ID_NAME + "pageSize=20&pageNum=1";
        } else {
            url = uapApiUrl + USER_ID_NAME + "pageSize=20&pageNum=1&nameLike=" + name;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());

        return sendQuery(headers, url);
    }

    /**
     * @Description: 根据用户姓名获取用户列表
     * @parameter: appId
     * @parameter: roleCode
     * @parameter: baseUrl
     * @return: java.util.List<com.uih.uplus.solar.shared.Model.UserData>
     */
    public List<UserData> getUserDataListById(String engineerId) {
        String url = uapApiUrl + USER_ID_NAME + "pageSize=20&pageNum=1&userId=" + engineerId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        return sendQuery(headers, url);
    }



    /**
     * @Description: 根据用户id获取用户信息
     * @parameter: userId
     * @parameter: baseUrl
     * @return: com.uih.uplus.solar.shared.Model.UserModel
     */
    public UserModel getUserById(Long userId) {
        UserModel userModel=new UserModel();
        List<Long> ids = Collections.singletonList(userId);
        List<UserModel> userModels = getUserDataByIdList(ids);
        if(!CollectionUtils.isEmpty(userModels)){
            userModel=userModels.get(0);
        }
        return userModel;
    }

    /**
     * @Description: 根据ids批量获取用户信息
     * @parameter: ids
     * @return: java.lang.String
     */
    public List<UserModel> getUserDataByIdList(List<Long> ids) {
        List<UserModel> userModels=new ArrayList<>();
        if(CollectionUtils.isEmpty(ids)){
            return  userModels;
        }
        ids = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        int i=0;
        while (i+20<=ids.size()){
            List<Long> subIds=ids.subList(i,i+20);
            List<UserModel> userModelMap = getUserMapByIdList(subIds);
            userModels.addAll(userModelMap);
            i+=20;
        }
        if(i<ids.size()){
            List<Long> subIds=ids.subList(i,ids.size());
            List<UserModel> userModelMap = getUserMapByIdList(subIds);
            userModels.addAll(userModelMap);
        }
        return userModels;
    }



    /**
     * 获取用户类型
     *
     * @return
     */
    public String getUserType() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());

        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);
        ParameterizedTypeReference<ResponseModel<String>> typeRef = new ParameterizedTypeReference<ResponseModel<String>>() {
        };
        try {
            ResponseModel<String> responseModel = restTemplate.exchange(uapApiUrl + USER_TYPE, HttpMethod.GET, httpEntity,
                                                                        typeRef).getBody();
            if (null == responseModel) {
                return "";
            }
            return responseModel.getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "获取用户类型异常!", e);
            return "";
        }
    }

    /**
     * 获取用户订阅solar系统的时间
     *
     * @return
     */
    public String getUserSubTime() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());

        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);
        ParameterizedTypeReference<ResponseModel<String>> typeRef = new ParameterizedTypeReference<ResponseModel<String>>() {
        };
        try {
            ResponseModel<String> responseModel = restTemplate.exchange(uapApiUrl + SUBSCRIPTION_TIME, HttpMethod.GET, httpEntity,
                                                                        typeRef).getBody();
            if (null == responseModel) {
                return "";
            }
            return responseModel.getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "获取solar系统订阅时间异常!", e);
            return "";
        }
    }


    /**
     * @Description: 根据ids批量获取用户信息，（个数<=20）
     * @parameter: ids
     * @return: java.util.Map<java.lang.Long, com.uih.uplus.solar.shared.Model.UserModel>
     */
    private List<UserModel> getUserMapByIdList(List<Long> ids) {
        List<UserModel> userModels = new ArrayList<>();
        HashMap<String, String> headers = MapUtil.newHashMap();
        headers.put(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        try {
            String url = UrlUtils.generateUrl("userIds=", uapApiUrl + USER_DETAILS_STR, ids);
            String result = HttpUtil.createGet(url).setReadTimeout(2000).addHeaders(headers).execute().body();
            Map<String,Object> resultMap=JsonUtils.deserialize(result, Map.class);
            if(resultMap.containsKey(MSG_CODE)&&((String)resultMap.get(MSG_CODE)).equals(SUCCESS_ID)){
                List data= (List) resultMap.get(DATA);
                for (Object itemMap:data){
                    Map<String,String> item=(Map<String, String>) itemMap;
                    UserModel userModel=JsonUtils.deserialize(JsonUtils.serialize(item),UserModel.class);
                    userModels.add(userModel);
                }
            }else {
                LogUtil.logDevError("根据ids批量获取用户信息返回失败: result={}", result);
            }
        }catch (Exception ex){
            LogUtil.logDevError("根据ids批量获取用户信息错误异常: {}", ex.toString());
        }
        return userModels;
    }


    /**
     * 发请求给租户平台
     *
     * @param headers 请求参数
     * @return list
     */
    private List<UserData> sendQuery(HttpHeaders headers, String url) {
        List<UserData> list = new ArrayList<>();
        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);
        try {
            String result = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class).getBody();
            Map<String, Object> resultMap = JsonUtils.deserialize(result, Map.class);

            if (null != resultMap && SUCCESS_ID.equals(resultMap.get(MSG_CODE))) {
                HashMap<String, Object> data = (HashMap<String, Object>)resultMap.get(DATA);
                List<HashMap<String, String>> dataList = (List<HashMap<String, String>>)data.get(DATA);
                for (Map<String, String> item : dataList) {
                    UserData userData = new UserData();
                    userData.setId(Long.parseLong(item.get("id")));
                    String nameStr = !StringUtils.isEmpty(item.get("ldapAccountName")) ? (item.get("name") + "(" + item.get(
                            "ldapAccountName") + ")") : item.get("name");
                    userData.setName(nameStr);
                    list.add(userData);
                }
            } else {
                logger.log(LogCst.ALERT_DEV_ERROR, "获取所有的用户返回失败：result={}", result);
            }
        } catch (Exception e) {
            logger.log(LogCst.ALERT_DEV_ERROR, "从租户平台查询第一责任人异常", e);
        }
        return list;
    }


    /**
     * 初始化工程师名字
     *
     * @param engineerIdList 工程师id集合
     */

    public Map<Long, String> getEngineerNameMap(List<Long> engineerIdList) {
        //工程师 id->name 映射表
        Map<Long, String> engineerMap = new HashMap<>();
        Map<Long, UserModel> userModelMap = this.getEngineerUserModelMap(engineerIdList);
        if (CollUtil.isEmpty(userModelMap)) {
            return engineerMap;
        }

        userModelMap.entrySet().stream().forEach(entry -> {
            UserModel userModel = entry.getValue();
            String userName = "";
            if (userModel != null) {
                userName = userModel.getName();
            }
            engineerMap.put(entry.getKey(), userName);
        });

        return engineerMap;
    }

    /**
     * 初始化工程师名字(名字+域账号显示)
     *
     * @param engineerIdList 工程师id集合
     */
    public Map<Long, String> getEngineerAccountNameMap(List<Long> engineerIdList) {
        //工程师 id->AccountName 映射表
        Map<Long, String> engineerMap = new HashMap<>();
        Map<Long, UserModel> userModelMap = this.getEngineerUserModelMap(engineerIdList);
        if (CollUtil.isEmpty(userModelMap)) {
            return engineerMap;
        }

        userModelMap.entrySet().stream().forEach(entry -> {
            UserModel userModel = entry.getValue();
            String userName = "";
            if (userModel != null) {
                String name=userModel.getName();
                String ldapAccountName = userModel.getLdapAccountName();
                userName = StringUtils.isEmpty(ldapAccountName) ? name : name+"("+ldapAccountName+")";
            }
            engineerMap.put(entry.getKey(), userName);
        });

        return engineerMap;
    }


    public String getUserNameById(Long userId) {
        UserModel userModel = this.getUserModelById(userId);
        if (Objects.isNull(userModel)) {
            return "";
        }
        return userModel.getName();
    }

    public String getUserAccountNameById(Long userId) {
        UserModel userModel = this.getUserModelById(userId);
        if (Objects.isNull(userModel)) {
            return "";
        }
        String name = userModel.getName();
        String ldapAccountName = userModel.getLdapAccountName();
        return StringUtils.isEmpty(ldapAccountName)?name : name+"("+ldapAccountName+")";
    }

    public UserModel getUserModelById(Long userId) {
        try {
            if (userId == null) {
                return null;
            }
            Map<Long, UserModel> nameMap = this.getEngineerUserModelMap(Arrays.asList(userId));
            return nameMap.getOrDefault(userId, null);
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_ERROR, "根据用户id获取用户失败userId={}", userId, e);
            return null;
        }

    }

    /**
     * 初始化工程师名字
     *
     * @param engineerIdList 工程师id集合
     */
    private Map<Long, UserModel> getEngineerUserModelMap(List<Long> engineerIdList) {
        //工程师 id->name 映射表
        Map<Long, UserModel> engineerMap = new HashMap<>();

        //优先从缓存中查找工程师id对应的名字
        engineerIdList.stream().filter(Objects::nonNull).forEach(engineerId -> {
            Object userObject = redisTemplate.opsForValue().get(RedisConst.USER_ID + engineerId);
            if (Objects.nonNull(userObject)) {
                engineerMap.put(engineerId, (UserModel)userObject);
            } else {
                engineerMap.put(engineerId, null);
            }
        });

        //获取在缓存中找不到工程师名字的id集合
        List<Long> emptyEngineerNameIdList = engineerMap.entrySet().stream().filter(entry -> Objects.isNull(entry.getValue()))
                .map(Map.Entry::getKey).distinct().collect(Collectors.toList());

        if (CollUtil.isEmpty(emptyEngineerNameIdList)) {
            return engineerMap;
        }

        //对于找不到工程师名字的id集合从uap中获取
        List<UserModel> userModels = this.getUserDataByIdList(emptyEngineerNameIdList);
        userModels.forEach(userModel -> {
            engineerMap.put(userModel.getUserId(), userModel);
            redisTemplate.opsForValue().set(RedisConst.USER_ID + userModel.getUserId(), userModel, RandomUtil.randomInt(60, 120),
                                            TimeUnit.MINUTES);
        });

        return engineerMap;
    }
    /**
     * 根据角色code获取角色列表
     *
     * @param roleCode 角色code
     * @param name 名称
     * @return list
     */
    public List<UserData> getPrincipalList(String roleCode, String name){
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        JSONObject map = new JSONObject();
        map.put("roleCode", roleCode);
        map.put("pageNum",1);
        map.put("pageSize",20);
        if(!StringUtils.isEmpty(name)){
            map.put("nameLike", name);
        }
        HttpEntity<JSONObject> request = new HttpEntity<>(map, headers);

        try {
            ResponseModel responseModel = restTemplate.postForEntity(uapApiUrl + CURRENT_ROLE_NAME, request, ResponseModel.class).getBody();
            List<UserData> userDataList = new ArrayList<>();
            if (null == responseModel||!U_000000.equalsIgnoreCase(responseModel.getCode())) {
                logger.log(LogCst.MANAGEMENT_DEV_INFO, "获取责任人列表失败!");
                return userDataList;
            }
            HashMap<String, Object> data = (HashMap<String, Object>)responseModel.getData();
            List<HashMap<String, String>> dataList = (List<HashMap<String, String>>)data.get(DATA);
            dataList.forEach(d->{
                UserData userData = new UserData();
                userData.setId(Long.parseLong(d.get("id")));
                String userName = d.get("name");
                String ldapAccountName = d.get("ldapAccountName");
                String nameStr = StringUtils.isEmpty(ldapAccountName) ? userName : (userName + "(" + ldapAccountName + ")") ;
                userData.setName(nameStr);
                userDataList.add(userData);
            });
            return userDataList;
        } catch(Exception ex) {
            logger.log(LogCst.MANAGEMENT_DEV_ERROR, "获取责任人列表异常!", ex);
            return new ArrayList<>();
        }
    }

    /**
     * 根据sopIds从租户平台批量获取用户信息(SOP数据同步使用)
     * @param ids sopId集合
     * @return 用户信息列表
     */
    public List<UserIdsModel> getUserIds(List<Long> ids){
        if(CollectionUtils.isEmpty(ids)){
            return new ArrayList<>();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<ResponseModel<ArrayList<UserIdsModel>>> typeRef = new ParameterizedTypeReference<ResponseModel<ArrayList<UserIdsModel>>>(){};

        String path = uapApiUrl + USER_SOP_ID;
        try{
            ResponseModel<ArrayList<UserIdsModel>> responseModel = restTemplate.exchange(
                    UrlUtils.generateUrl("sopId=", path, ids), HttpMethod.GET, httpEntity, typeRef).getBody();
            List<UserIdsModel> userIds = responseModel.getData();
            if(CollectionUtils.isEmpty(userIds)){
                logger.log(LogCst.MIGRATION_DEV_ERROR,"根据sopIds从租户平台批量获取用户id为空！");
            }
            return userIds;
        }catch (Exception e){
            logger.log(LogCst.MIGRATION_DEV_ERROR,"根据sopIds从租户平台批量获取用户id异常！",e);
            return new ArrayList<>();
        }
    }

}




