package com.uih.uplus.solar.shared.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.uih.uplus.common.utils.basic.CollUtil;
import com.uih.uplus.common.utils.basic.StrUtil;
import com.uih.uplus.solar.shared.Model.AreaInfoModel;
import com.uih.uplus.solar.shared.Model.AreaModel;
import com.uih.uplus.solar.shared.Model.AuthCustomerModel;
import com.uih.uplus.solar.shared.Model.DepartmentTreeModel;
import com.uih.uplus.solar.shared.Model.OrganizationIdsModel;
import com.uih.uplus.solar.shared.Model.PageResult;
import com.uih.uplus.solar.shared.Model.ResponseModel;
import com.uih.uplus.solar.shared.Model.UapDepartmentModel;
import com.uih.uplus.solar.shared.Model.UapUserRoleModel;
import com.uih.uplus.solar.shared.constant.LogCst;
import com.uih.uplus.solar.shared.constant.RedisConst;
import com.uih.uplus.solar.shared.constant.RoleConst;
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
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.uih.log.UIHLog;


/**
 * @ClassName CustomerUtils
 * @Description CustomerUtils
 * @Author zhou.yang
 * @Date 2020/7/1 11:05
 */
@UIHLog
@Component
@ConditionalOnBean(name = {"tokenUtil"})
public class CustomerUtils {

    @Autowired
    protected HttpServletRequest request;

    @Value("${api.uapApiUrl:http}")
    public String uapApiUrl;

    @Autowired
    private TokenUtil tokenUtil;


    private static final String MSG_CODE = "msgCode";
    private static final String SUCCESS_ID = "success.id";
    private static final String DATA = "data";

    private String authCustomerPagePath = UrlVersionConst.PORTAL_API_V_1 + "/organization/query/page";
    private String authOrgNameAreaProvince = UrlVersionConst.PORTAL_API_V_1 + "/third/org/name-area-province";
    private String authCustomerListPath = UrlVersionConst.PORTAL_API_V_1 + "/organization/query/list";
    private String authCustomerByUserId = UrlVersionConst.PORTAL_API_V_1 + "/organization/user";
    private String authCountry = UrlVersionConst.PORTAL_API_V_1 + "/address/country";
    private String authProvince = UrlVersionConst.PORTAL_API_V_1 + "/address/province";
    private String authCity = UrlVersionConst.PORTAL_API_V_1 + "/address/city";
    private String authDistrict = UrlVersionConst.PORTAL_API_V_1 + "/address/district";
    private String authCountryProvince = UrlVersionConst.PORTAL_API_V_1 + "/organization/sop/name-area";
    private String authAreaInfo = UrlVersionConst.PORTAL_API_V_1 + "/organization/area-info/batch";
    private String authDepartment = UrlVersionConst.PORTAL_API_V_1 + "/organization/department/tree";
    private static final String CURRENT_DEPARTMENT = UrlVersionConst.PORTAL_API_V_1 + "/organization/current-department";
    private static final String DEPARTMENT_BATCH_INFO = UrlVersionConst.PORTAL_API_V_1 + "/organization/department/batch";
    private static final String DEPARTMENT_USER_ROLE = UrlVersionConst.PORTAL_API_V_1 + "/organization/department/user-role";

    private static final String CURRENT_ORG = UrlVersionConst.PORTAL_API_V_1 + "/organization/current";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    public List<AuthCustomerModel> getCustomerModels(List<Long> customerIds) {
        List<AuthCustomerModel> authCustomerModelList = new ArrayList<>();
        int i = 0;
        while (i + 300 < customerIds.size()) {
            List<Long> ids = customerIds.subList(i, i + 300);
            List<AuthCustomerModel> authCustomerModelSubList = this.authQueryByIds(ids);
            authCustomerModelList.addAll(authCustomerModelSubList);
            i = i + 300;
        }
        if (i < customerIds.size()) {
            List<Long> ids = customerIds.subList(i, customerIds.size());
            List<AuthCustomerModel> authCustomerModelSubList = this.authQueryByIds(ids);
            authCustomerModelList.addAll(authCustomerModelSubList);
        }
        return authCustomerModelList;
    }

    /**
     * 根据IDs从租户平台批量获取客户信息
     *
     * @param ids 客户ID集合
     * @return 客户信息列表
     */
    public List<AuthCustomerModel> authQueryByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        //增加缓存
        ids = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<AuthCustomerModel> authCustomerModels = new ArrayList<>();
        List<Long> noNameIds = new ArrayList<>();
        for (long id : ids) {
            Object msgObjAuthCustomer = redisTemplate.opsForValue().get(RedisConst.CUSTOMER_MODEL + id);
            if (msgObjAuthCustomer == null) {
                noNameIds.add(id);
            } else {
                authCustomerModels.add((AuthCustomerModel)msgObjAuthCustomer);
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>>() {
                };

        if (!CollectionUtils.isEmpty(noNameIds)) {
            try {
                String params = UrlUtils.generateUrl("id=", uapApiUrl + authCustomerListPath + "?", noNameIds);
                ResponseModel<ArrayList<AuthCustomerModel>> responseModel = restTemplate.exchange(params, HttpMethod.GET,
                                                                                                  httpEntity, typeRef).getBody();
                if (null == responseModel) {
                    return new ArrayList<>();
                }
                for (AuthCustomerModel authCustomerModel : responseModel.getData()) {
                    if (!ObjUtils.checkObjectHasNullField(authCustomerModel)) {
                        redisTemplate.opsForValue().set(RedisConst.CUSTOMER_MODEL + authCustomerModel.getId(), authCustomerModel,
                                                        RandomUtil.randomInt(60, 120), TimeUnit.MINUTES);
                    }
                }
                authCustomerModels.addAll(responseModel.getData());
            } catch (Exception e) {
                logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据IDs从租户平台批量获取客户信息异常", e);
                return new ArrayList<>();
            }
        }
        return authCustomerModels;
    }

    /**
     * 从租户平台查询机构列表
     * @param name 机构名称
     * @return 客户信息列表
     */
    public List<AuthCustomerModel> authQueryList(String name) {
        HttpHeaders headers = new HttpHeaders();

        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());

        String params;
        if (StringUtils.isEmpty(name)) {
            params = "?pageSize=20&pageNum=1";
        } else {
            params = "?pageSize=20&pageNum=1&keyword=" + name;
        }

        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);
        ParameterizedTypeReference<ResponseModel<PageResult<AuthCustomerModel>>> typeRef = new ParameterizedTypeReference<ResponseModel<PageResult<AuthCustomerModel>>>() {
        };
        try {
            ResponseModel<PageResult<AuthCustomerModel>> responseModel = restTemplate.exchange(
                    uapApiUrl + authCustomerPagePath + params, HttpMethod.GET, httpEntity, typeRef).getBody();

            if (null == responseModel) {
                return new ArrayList<>();
            }
            return responseModel.getData().getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据名称从租户平台查询机构列表异常", e);
            return new ArrayList<>();
        }

    }


    /**
     * 从租户平台查询机构列表
     *
     * @param nameLike
     * @param areaType
     * @param provinceIdList
     * @return
     */
    public List<AuthCustomerModel> authOrgNameAreaProvince(String nameLike, String areaType, List<Long> provinceIdList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pageNum", 1);
        jsonObject.put("pageSize", 20);
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(nameLike)) {
            jsonObject.put("nameLike", nameLike);
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(areaType)) {
            jsonObject.put("areaType", areaType);
        }
        if (CollUtil.isNotEmpty(provinceIdList)) {
            jsonObject.put("provinceIdList", provinceIdList);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity httpEntity = new HttpEntity<>(jsonObject, headers);
        try {
            ResponseModel responseModel = restTemplate.postForEntity(uapApiUrl + authOrgNameAreaProvince, httpEntity,
                                                                     ResponseModel.class).getBody();

            if (null == responseModel || null == responseModel.getData()) {
                return new ArrayList<>();
            }
            Object mapData = ((LinkedHashMap)responseModel.getData()).getOrDefault("data", null);
            if (Objects.isNull(mapData)) {
                return new ArrayList<>();
            }
            List<AuthCustomerModel> authCustomerModels = JSON.parseArray(JSON.toJSONString(mapData), AuthCustomerModel.class);
            return authCustomerModels;
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据国家省份id从租户平台获取客户Ids异常", e);
            return new ArrayList<>();
        }

    }

    /**
     * 根据机构id从租户平台查询机构名称
     *
     * @param id 机构id
     * @return 客户名称
     */
    public ArrayList<AuthCustomerModel> authQueryCustomer(String id) {
        HttpHeaders headers = new HttpHeaders();

        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());

        String params = "";
        if (!StringUtils.isEmpty(id)) {
            params = "?id=" + id;
        }

        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);
        ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>>() {
                };
        try {
            ResponseModel<ArrayList<AuthCustomerModel>> responseModel = restTemplate.exchange(
                    uapApiUrl + authCustomerListPath + params, HttpMethod.GET, httpEntity, typeRef).getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            return responseModel.getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据机构ID从租户平台查询机构信息异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据国家省市区id从租户平台获取客户Ids
     *
     * @param countryId  国家id
     * @param provinceId 省份id
     * @param cityId     市id
     * @param districtId 区/县id
     * @return 客户ID集合
     */
    public List<Long> authQueryIdsByArea(Long countryId, Long provinceId, Long cityId, Long districtId, String customerName) {
        ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>>() {
                };

        String params = "?";
        if (!StringUtils.isEmpty(countryId)) {
            params = params + "countryId=" + countryId;
        }
        if (!StringUtils.isEmpty(provinceId)) {
            params = params + "&provinceId=" + provinceId;
        }
        if (!StringUtils.isEmpty(cityId)) {
            params = params + "&cityId=" + cityId;
        }
        if (!StringUtils.isEmpty(districtId)) {
            params = params + "&districtId=" + districtId;
        }
        if (!StringUtils.isEmpty(customerName)) {
            params = params + "&keyword=" + customerName;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<HashMap<String, Long>> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseModel<ArrayList<AuthCustomerModel>> responseModel = restTemplate.exchange(
                    uapApiUrl + authCustomerListPath + params, HttpMethod.GET, httpEntity, typeRef).getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            List<AuthCustomerModel> authCustomerModelList = responseModel.getData();
            return authCustomerModelList.stream().map(AuthCustomerModel::getId).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据国家省市区id从租户平台获取客户Ids异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户所在机构信息
     * @return 机构信息
     */
    public List<AuthCustomerModel> authQueryByUserId() {
        //使用uap真实的token判断是否是第三方凭据
        if (StringUtils.isEmpty(tokenUtil.getRealUserId())) {//走客户端凭据流程，userId為null
            return this.getClientOrganization();
        }
        ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>>() {
                };
        Map<String, Long> params = new HashMap<>();
        params.put("accountId", Long.valueOf(tokenUtil.getUserId()));
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<Map<String, Long>> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseModel<ArrayList<AuthCustomerModel>> responseModel = restTemplate.exchange(
                    uapApiUrl + authCustomerByUserId + "?accountId={accountId}", HttpMethod.GET, httpEntity, typeRef, params)
                    .getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            return responseModel.getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据用户ID获取所在机构信息异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 用户当前租户下的机构列表
     * @return
     */
    public List<AuthCustomerModel> currentOrgList() {
        //使用uap真实的token判断是否是第三方凭据
        if (StringUtils.isEmpty(tokenUtil.getRealUserId())) {//走客户端凭据流程，userId為null
            return this.getClientOrganization();
        }
        ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AuthCustomerModel>>>() {
                };
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<Map<String, Long>> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseModel<ArrayList<AuthCustomerModel>> responseModel = restTemplate.exchange(uapApiUrl + CURRENT_ORG,
                                                                                              HttpMethod.GET, httpEntity, typeRef)
                    .getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            return responseModel.getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据用户ID获取所在机构信息异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取国家列表
     *
     * @return 国家列表
     */
    public List<AreaModel> authQueryCountrys() {
        ParameterizedTypeReference<ResponseModel<ArrayList<AreaModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AreaModel>>>() {
                };
        HttpHeaders headers = new HttpHeaders();

        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseModel<ArrayList<AreaModel>> responseModel = restTemplate.exchange(uapApiUrl + authCountry, HttpMethod.GET,
                                                                                      httpEntity, typeRef).getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            return responseModel.getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "获取国家列表异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取省份列表
     *
     * @param countryId 国家ID
     * @return 省份列表
     */
    public List<AreaModel> authQueryProvinces(Long countryId) {

        ArrayList<AreaModel> redisDatas = (ArrayList<AreaModel>)redisTemplate.opsForValue().get(
                RedisConst.CUSTOMER_PROVINCE + countryId);
        if (CollUtil.isNotEmpty(redisDatas)) {
            return redisDatas;
        }

        ParameterizedTypeReference<ResponseModel<ArrayList<AreaModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AreaModel>>>() {
                };
        Map<String, Long> params = new HashMap<>();
        params.put("countryId", countryId);
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<MultiValueMap<String, Long>> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseModel<ArrayList<AreaModel>> responseModel = restTemplate.exchange(
                    uapApiUrl + authProvince + "?countryId={countryId}", HttpMethod.GET, httpEntity, typeRef, params).getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            ArrayList<AreaModel> data = responseModel.getData();
            if (CollUtil.isNotEmpty(data)) {
                redisTemplate.opsForValue().set(RedisConst.CUSTOMER_PROVINCE + countryId, data, RandomUtil.randomInt(60, 120),
                                                TimeUnit.MINUTES);
            }

            return data;
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "获取省份列表异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取城市列表
     *
     * @param provinceId 省份ID
     * @return 城市列表
     */
    public List<AreaModel> authQueryCitys(Long provinceId) {
        ParameterizedTypeReference<ResponseModel<ArrayList<AreaModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AreaModel>>>() {
                };
        Map<String, Long> params = new HashMap<>();
        params.put("provinceId", provinceId);
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<MultiValueMap<String, Long>> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseModel<ArrayList<AreaModel>> responseModel = restTemplate.exchange(
                    uapApiUrl + authCity + "?provinceId={provinceId}", HttpMethod.GET, httpEntity, typeRef, params).getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            return responseModel.getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "获取城市列表异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取区/县列表
     *
     * @param cityId 城市ID
     * @return 区/县列表
     */
    public List<AreaModel> authQueryDistricts(Long cityId) {
        ParameterizedTypeReference<ResponseModel<ArrayList<AreaModel>>> typeRef =
                new ParameterizedTypeReference<ResponseModel<ArrayList<AreaModel>>>() {
                };
        Map<String, Long> params = new HashMap<>();
        params.put("cityId", cityId);
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<MultiValueMap<String, Long>> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseModel<ArrayList<AreaModel>> responseModel = restTemplate.exchange(
                    uapApiUrl + authDistrict + "?cityId={cityId}", HttpMethod.GET, httpEntity, typeRef, params).getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            return responseModel.getData();
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "获取区/县列表异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取机构ID集合
     * @param countryId   国家ID
     * @param provinceIds 省份ID集合
     * @param customerName  机构名称
     * @return 客户ID集合
     */
    public List<Long> authQueryIdsByCountryProvince(Long countryId, List<Long> provinceIds, String customerName) {
        JSONObject jsonObject = new JSONObject();
        if (null != countryId) {
            jsonObject.put("countryId", countryId);
        }
        if (!CollectionUtils.isEmpty(provinceIds)) {
            jsonObject.put("provinceIds", provinceIds);
        }
        if (!StringUtils.isEmpty(customerName)) {
            jsonObject.put("nameLike", customerName);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity httpEntity = new HttpEntity<>(jsonObject, headers);
        try {
            ResponseModel responseModel = restTemplate.postForEntity(uapApiUrl + authCountryProvince, httpEntity,
                                                                     ResponseModel.class).getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            ArrayList authCustomerModels = (ArrayList)responseModel.getData();
            List<Long> ids = new ArrayList<>();
            for (Object authCustomerModel : authCustomerModels) {
                ids.add(Long.valueOf(((LinkedHashMap)authCustomerModel).get("id").toString()));
            }
            return ids;
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据国家省份id从租户平台获取客户Ids异常", e);
            return new ArrayList<>();
        }
    }

    public List<AreaInfoModel> getAreaInfoModels(List<Long> customerIds) {
        List<AreaInfoModel> authAreaInfoModelList = new ArrayList<>();
        int i = 0;
        while (i + 300 < customerIds.size()) {
            List<Long> ids = customerIds.subList(i, i + 300);
            List<AreaInfoModel> authAreaInfoModelSubList = this.authQueryAreaInfoByOrgId(ids);
            authAreaInfoModelList.addAll(authAreaInfoModelSubList);
            i = i + 300;
        }
        if (i < customerIds.size()) {
            List<Long> ids = customerIds.subList(i, customerIds.size());
            List<AreaInfoModel> authAreaInfoModelSubList = this.authQueryAreaInfoByOrgId(ids);
            authAreaInfoModelList.addAll(authAreaInfoModelSubList);
        }
        return authAreaInfoModelList;
    }

    /**
     * 获取用户部门id
     *
     * @return
     */
    public Long authGetCurrentDepartmentId() {
        //1351764174947803168L
        Long customerId = this.getCustomerId();
        String params = "";
        if (!Objects.isNull(customerId)) {
            params = "?orgId=" + customerId;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<MultiValueMap<String, Long>> httpEntity = new HttpEntity<>(headers);

        try {
            ResponseModel responseModel = restTemplate.exchange(uapApiUrl + CURRENT_DEPARTMENT + params, HttpMethod.GET,
                                                                httpEntity, ResponseModel.class, params).getBody();
            if (null == responseModel) {
                return null;
            } else {
                Map data = (HashMap)responseModel.getData();
                Long res = data == null ? null : Long.valueOf(data.getOrDefault("id", "0").toString());
                return res;
            }
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据机构ID从会话中用户所在部门信息异常", e);
            return null;
        }
    }

    /**
     * 获取机构所在位置信息
     *
     * @param orgIds 国家ID
     * @return 机构所在位置信息集合
     */
    public List<AreaInfoModel> authQueryAreaInfoByOrgId(List<Long> orgIds) {
        if (CollectionUtils.isEmpty(orgIds)) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>(orgIds);
        ids.removeAll(Collections.singleton(null));
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity httpEntity = new HttpEntity<>(ids, headers);
        try {
            ResponseModel responseModel = restTemplate.postForEntity(uapApiUrl + authAreaInfo, httpEntity, ResponseModel.class)
                    .getBody();
            if (null == responseModel || null == responseModel.getData()) {
                return new ArrayList<>();
            }
            return JSON.parseArray(JSON.toJSONString(responseModel.getData()), AreaInfoModel.class);
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据机构id从租户平台获取机构所在位置信息异常！", e);
            return new ArrayList<>();
        }
    }

    private static final String U_000000 = "U000000";

    public List<UapUserRoleModel> authDepartmentUserRole(List<Long> depIds) {
        List<UapUserRoleModel> resAll = new ArrayList<>();
        List<List<Long>> lists = SubListUtils.generateListGroup(depIds, 100);
        for (int i = 0; i < lists.size(); i++) {
            List<UapUserRoleModel> subResult = getDepartAssetAdminComparingByDepId(lists.get(i));
            if (subResult != null) {
                resAll.addAll(subResult);
            }
        }
        return resAll;
    }


    /**
     * 获取第一个depId的资产责任人数据
     *
     * @param departmentIds
     * @return
     */
    public List<UapUserRoleModel> getDepartAssetAdminComparingByDepId(List<Long> departmentIds) {
        List<UapUserRoleModel> res = new ArrayList<>();
        List<Long> ids = departmentIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> noNameIds = new ArrayList<>();
        for (long id : ids) {
            Object msgObjAuthCustomer = redisTemplate.opsForValue().get(RedisConst.DEPARTMENT_USR_MODEL + id);
            if (msgObjAuthCustomer == null) {
                noNameIds.add(id);
            } else {
                res.add((UapUserRoleModel)msgObjAuthCustomer);
            }
        }
        if (CollUtil.isEmpty(noNameIds)) {
            return res;
        }
        List<UapUserRoleModel> uapUserRoleModelList = getDepartAssetAdminByDepIds(departmentIds);
        ArrayList<UapUserRoleModel> filterUapList = uapUserRoleModelList.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(UapUserRoleModel::getDeptId))), ArrayList::new));
        for (UapUserRoleModel authCustomerModel : filterUapList) {
            if (authCustomerModel != null && authCustomerModel.getId() != null) {
                redisTemplate.opsForValue().set(RedisConst.DEPARTMENT_USR_MODEL + authCustomerModel.getDeptId(),
                                                authCustomerModel, RandomUtil.randomInt(1, 5), TimeUnit.MINUTES);
            }
        }
        res.addAll(filterUapList);
        return res;
    }

    /**
     * 根据depid批量获取资产责任人角色用户信息
     *
     * @param departmentIds
     * @return
     */
    public List<UapUserRoleModel> getDepartAssetAdminByDepIds(List<Long> departmentIds) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        JSONObject map = new JSONObject();
        map.put("deptIds", departmentIds);
        map.put("roleCode", RoleConst.ASSETS_ADMIN);
        HttpEntity request = new HttpEntity<>(map, headers);
        try {
            ResponseModel responseModel = restTemplate.postForEntity(uapApiUrl + DEPARTMENT_USER_ROLE, request,
                                                                     ResponseModel.class).getBody();
            if (null == responseModel || !U_000000.equalsIgnoreCase(responseModel.getCode())) {
                return new ArrayList<>();
            }
            ArrayList uapDepartmentModelListMap = (ArrayList)responseModel.getData();
            List<UapUserRoleModel> uapUserRoleModelList = JSON.parseArray(JSON.toJSONString(uapDepartmentModelListMap),
                                                                          UapUserRoleModel.class);
            return uapUserRoleModelList;

        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据机构id从租户平台获取获取资产责任人角色用户信息异常！depIds={},e={}", departmentIds, e);
            return new ArrayList<>();
        }
    }

    public List<UapDepartmentModel> authDepartMentBatch(List<Long> departmentIds) {

        List<UapDepartmentModel> res = new ArrayList<>();
        if (CollectionUtils.isEmpty(departmentIds)) {
            return new ArrayList<>();
        }
        List<Long> ids = departmentIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> noNameIds = new ArrayList<>();
        for (long id : ids) {
            Object msgObjAuthCustomer = redisTemplate.opsForValue().get(RedisConst.DEPARTMENT_MODEL + id);
            if (msgObjAuthCustomer == null) {
                noNameIds.add(id);
            } else {
                res.add((UapDepartmentModel)msgObjAuthCustomer);
            }
        }


        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (CollUtil.isNotEmpty(noNameIds)) {
            HttpEntity httpEntity = new HttpEntity<>(noNameIds, headers);
            try {
                ResponseModel responseModel = restTemplate.postForEntity(uapApiUrl + DEPARTMENT_BATCH_INFO, httpEntity,
                                                                         ResponseModel.class).getBody();
                if (null == responseModel) {
                    return new ArrayList<>();
                }
                ArrayList uapDepartmentModelListMap = (ArrayList)responseModel.getData();
                List<UapDepartmentModel> uapDepartmentModelList = JSON.parseArray(JSON.toJSONString(uapDepartmentModelListMap),
                                                                                  UapDepartmentModel.class);
                for (UapDepartmentModel authCustomerModel : uapDepartmentModelList) {
                    if (authCustomerModel != null && authCustomerModel.getId() != null) {
                        redisTemplate.opsForValue().set(RedisConst.DEPARTMENT_MODEL + authCustomerModel.getId(),
                                                        authCustomerModel, RandomUtil.randomInt(60, 120), TimeUnit.MINUTES);
                    }
                }
                res.addAll(uapDepartmentModelList);
            } catch (Exception e) {
                logger.log(LogCst.MANAGEMENT_DEV_INFO, "根据机构id从租户平台获取部门信息异常！", e);
                return new ArrayList<>();
            }
        }
        return res;
    }

    public List authQueryDepartmentByOrgId(Long orgId) {
        Map<String, Long> params = new HashMap<>();
        params.put("orgId", orgId);
        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<MultiValueMap<String, Long>> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseModel responseModel = restTemplate.exchange(uapApiUrl + authDepartment + "?orgId={orgId}", HttpMethod.GET,
                                                                httpEntity, ResponseModel.class, params).getBody();
            if (null == responseModel) {
                return new ArrayList<>();
            }
            ArrayList dataList = (ArrayList)responseModel.getData();
            return extractDepartmentInfo(dataList);
        } catch (Exception e) {
            logger.log(LogCst.MANAGEMENT_DEV_INFO, "获取机构部门信息异常！", e);
            return new ArrayList<>();
        }
    }

    private List<DepartmentTreeModel> extractDepartmentInfo(List<DepartmentTreeModel> departmentTreeModels) {
        ArrayList<DepartmentTreeModel> departmentTreeModelList = new ArrayList<>();
        for (Object data : departmentTreeModels) {
            DepartmentTreeModel departmentTreeModel = new DepartmentTreeModel();
            Object idObj = ((LinkedHashMap)data).get("id");
            Object parentIdObj = ((LinkedHashMap)data).get("parentId");
            Object nameObj = ((LinkedHashMap)data).get("name");
            Long id = null != idObj ? Long.valueOf(idObj.toString()) : null;
            String name = null != nameObj ? nameObj.toString() : "";
            Long parentId = null != parentIdObj ? Long.valueOf(parentIdObj.toString()) : null;

            ArrayList children = (ArrayList)((LinkedHashMap)data).get("children");
            departmentTreeModel.setKey(id);
            departmentTreeModel.setValue(name);
            departmentTreeModel.setParentId(parentId);
            List departmentChildren = new ArrayList<>();
            if (!CollectionUtils.isEmpty(children)) {
                departmentChildren = extractDepartmentInfo(children);
            }
            departmentTreeModel.setChildren(departmentChildren);
            departmentTreeModelList.add(departmentTreeModel);
        }
        return departmentTreeModelList;
    }


    /**
     * @Description: 获取【客户端凭据】对应的机构信息
     * @return: String
     */
    private List<AuthCustomerModel> getClientOrganization() {
        List<AuthCustomerModel> authCustomerModels = new ArrayList<>();
        try {
            List<Long> orgIds = this.getClientOrganizationIds();
            HashMap<String, String> headers = MapUtil.newHashMap();
            headers.put(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
            String url = UrlUtils.generateUrl("id=", uapApiUrl + authCustomerListPath + "?", orgIds);
            String result = HttpUtil.createGet(url).setReadTimeout(2000).addHeaders(headers).execute().body();
            Map resultMap = JsonUtils.deserialize(result, Map.class);
            if (resultMap != null && resultMap.containsKey(MSG_CODE) && resultMap.get(MSG_CODE).equals(SUCCESS_ID)) {
                List data = (List)resultMap.get(DATA);
                for (Object itemMap : data) {
                    Map<String, String> item = (Map<String, String>)itemMap;
                    AuthCustomerModel authCustomerModel = JsonUtils.deserialize(JsonUtils.serialize(item),
                                                                                AuthCustomerModel.class);
                    authCustomerModels.add(authCustomerModel);
                }
            } else {
                LogUtil.logDevError("【客户端凭据流程】根据机构ids批量获取机构信息返回失败：result={}", result);
            }
        } catch (Exception ex) {
            LogUtil.logDevError("【客户端凭据】根据ids批量获取机构信息异常：{}", ex.toString());
        }
        return authCustomerModels;
    }

    /**
     * 获取【客户端凭据】的机构id
     *
     * @return
     */
    private List<Long> getClientOrganizationIds() {
        String clientCredentialInfo = tokenUtil.getClientCredentialInfo();
        List<Long> organizationIds = new ArrayList<>();
        if (!StringUtils.isEmpty(clientCredentialInfo)) {
            ResponseModel clientCredentialData = JsonUtils.deserialize(clientCredentialInfo, ResponseModel.class);
            if (ObjectUtils.isEmpty(clientCredentialData) || !clientCredentialData.getMsgCode().equals(SUCCESS_ID)) {
                return organizationIds;
            }

            Object tenantOrganizationBindDataList = ((LinkedHashMap)(clientCredentialData.getData())).get(
                    "tenantOrganizationBindDataList");
            if (ObjectUtils.isEmpty(tenantOrganizationBindDataList)) {
                return organizationIds;
            }
            Object tenantOrganizationBindData = ((ArrayList)tenantOrganizationBindDataList).get(0);
            ArrayList orgIds = (ArrayList)((LinkedHashMap)tenantOrganizationBindData).get("organizationIds");
            for (Object ob : orgIds) {
                organizationIds.add(Long.parseLong(ob.toString()));
            }
        }
        return organizationIds;
    }


    public Long getCustomerId() {
        // 从request统一获取customerId
        String uri = request.getRequestURI();
        String orgId = request.getHeader("orgId");
        LogUtil.logDevInfo("uri==={},orgId==={}", uri, orgId);
        if (StrUtil.isNotBlank(orgId)) {
            return Long.valueOf(orgId);
        }
        //兼容小程序切换
        List<AuthCustomerModel> customers = this.authQueryByUserId();
        Long customerId = null;
        if (!CollectionUtils.isEmpty(customers)) {
            customerId = customers.get(0).getId();
            return customerId;
        }
        LogUtil.logDevInfo("未查询到所在机构信息");
        return customerId;
    }

    /**
     * 根据IDs从租户平台批量获取机构信息（SOP数据同步使用）
     * @param ids sopId集合
     * @return 机构信息列表
     */
    public  List<OrganizationIdsModel> getOrganizationIds(List<Long> ids){
        if(CollectionUtils.isEmpty(ids)){
            return new ArrayList<>();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(TokenCst.AUTHORIZATION, tokenUtil.getTokenBearer());
        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<ResponseModel<ArrayList<OrganizationIdsModel>>> typeRef = new ParameterizedTypeReference<ResponseModel<ArrayList<OrganizationIdsModel>>>(){};

        String path = uapApiUrl + authCustomerListPath + "?";
        try{
            ResponseModel<ArrayList<OrganizationIdsModel>> responseModel = restTemplate.exchange(
                    UrlUtils.generateUrl("sopId=", path, ids), HttpMethod.GET, httpEntity, typeRef).getBody();
            List<OrganizationIdsModel> organizationIds= responseModel.getData();
            if(CollectionUtils.isEmpty(organizationIds)) {
                logger.log(LogCst.MIGRATION_DEV_ERROR,"根据sopId从租户平台批量获取机构id为空！");
            }
            return organizationIds;
        }catch (Exception e){
            logger.log(LogCst.MIGRATION_DEV_ERROR,"根据sopId从租户平台批量获取机构id异常！",e);
            return new ArrayList<>();
        }
    }


}
