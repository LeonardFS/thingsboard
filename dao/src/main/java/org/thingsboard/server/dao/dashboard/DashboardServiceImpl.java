/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static org.thingsboard.server.dao.service.Validator.validateId;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.sql.TenantEntity;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.tenant.JpaTenantDao;
import org.thingsboard.server.dao.user.UserDao;

import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;

import lombok.extern.slf4j.Slf4j;

@Service("DashboardDaoService")
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl extends AbstractEntityService implements DashboardService {

    public static final String INCORRECT_DASHBOARD_ID = "Incorrect dashboardId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private ImageService imageService;

    @Autowired
    private DataValidator<Dashboard> dashboardValidator;

    @Autowired
    protected TbTransactionalCache<DashboardId, String> cache;

    @Autowired
    private EntityCountService countService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    protected void publishEvictEvent(DashboardTitleEvictEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }

    @TransactionalEventListener(classes = DashboardTitleEvictEvent.class)
    public void handleEvictEvent(DashboardTitleEvictEvent event) {
        cache.evict(event.getKey());
    }

    @Autowired
    private JpaTenantDao jpaTenantDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private DashboardService dashboardService;

    @Override
    public Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardById [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardDao.findById(tenantId, dashboardId.getId());
    }

    @Override
    public ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardByIdAsync [{}]", dashboardId);
        validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardDao.findByIdAsync(tenantId, dashboardId.getId());
    }

    @Override
    public DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoById [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardInfoDao.findById(tenantId, dashboardId.getId());
    }

    @Override
    public String findDashboardTitleById(TenantId tenantId, DashboardId dashboardId) {
        return cache.getAndPutInTransaction(dashboardId,
                () -> dashboardInfoDao.findTitleById(tenantId.getId(), dashboardId.getId()), true);
    }

    @Override
    public ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoByIdAsync [{}]", dashboardId);
        validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardInfoDao.findByIdAsync(tenantId, dashboardId.getId());
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard, boolean doValidate) {
        return doSaveDashboard(dashboard, doValidate);
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard) {
        return doSaveDashboard(dashboard, true);
    }

    private Dashboard doSaveDashboard(Dashboard dashboard, boolean doValidate) {
        log.trace("Executing saveDashboard [{}]", dashboard);
        if (doValidate) {
            dashboardValidator.validate(dashboard, DashboardInfo::getTenantId);
        }
        try {
            imageService.replaceBase64WithImageUrl(dashboard);
            var saved = dashboardDao.save(dashboard.getTenantId(), dashboard);
            publishEvictEvent(new DashboardTitleEvictEvent(saved.getId()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId())
                    .entityId(saved.getId()).created(dashboard.getId() == null).build());
            if (dashboard.getId() == null) {
                countService.publishCountEntityEvictEvent(saved.getTenantId(), EntityType.DASHBOARD);
            }
            return saved;
        } catch (Exception e) {
            if (dashboard.getId() != null) {
                publishEvictEvent(new DashboardTitleEvictEvent(dashboard.getId()));
            }
            checkConstraintViolation(e, "dashboard_external_id_unq_key", "Dashboard with such external id already exists!");
            throw e;
        }
    }



    @Override
    public Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't assign dashboard to non-existent customer!");
        }
        if (!customer.getTenantId().getId().equals(dashboard.getTenantId().getId())) {
            throw new DataValidationException("Can't assign dashboard to customer from different tenant!");
        }
        if (dashboard.addAssignedCustomer(customer)) {
            try {
                createRelation(tenantId, new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (Exception e) {
                log.warn("[{}] Failed to create dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    @Override
    public Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboard from non-existent customer!");
        }
        if (dashboard.removeAssignedCustomer(customer)) {
            try {
                deleteRelation(tenantId, new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (Exception e) {
                log.warn("[{}] Failed to delete dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    private Dashboard updateAssignedCustomer(TenantId tenantId, DashboardId dashboardId, Customer customer) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        if (dashboard.updateAssignedCustomer(customer)) {
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    @Override
    @Transactional
    public void deleteDashboard(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing deleteDashboard [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        deleteEntityRelations(tenantId, dashboardId);
        try {
            dashboardDao.removeById(tenantId, dashboardId.getId());
            publishEvictEvent(new DashboardTitleEvictEvent(dashboardId));
            countService.publishCountEntityEvictEvent(tenantId, EntityType.DASHBOARD);
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(dashboardId).build());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_dashboard_device_profile")) {
                throw new DataValidationException("The dashboard referenced by the device profiles cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteDashboardsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDashboardsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDashboardsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public void unassignCustomerDashboards(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDashboards, customerId [{}]", customerId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboards from non-existent customer!");
        }
        new CustomerDashboardsUnassigner(customer).removeEntities(tenantId, customer);
    }

    @Override
    public void updateCustomerDashboards(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing updateCustomerDashboards, customerId [{}]", customerId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't update dashboards for non-existent customer!");
        }
        new CustomerDashboardsUpdater(customer).removeEntities(tenantId, customer);
    }

    @Override
    public Dashboard assignDashboardToEdge(TenantId tenantId, DashboardId dashboardId, EdgeId edgeId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Edge edge = edgeDao.findById(tenantId, edgeId.getId());
        if (edge == null) {
            throw new DataValidationException("Can't assign dashboard to non-existent edge!");
        }
        if (!edge.getTenantId().equals(dashboard.getTenantId())) {
            throw new DataValidationException("Can't assign dashboard to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create dashboard relation. Edge Id: [{}]", dashboardId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(dashboardId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        return dashboard;
    }

    @Override
    public Dashboard unassignDashboardFromEdge(TenantId tenantId, DashboardId dashboardId, EdgeId edgeId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Edge edge = edgeDao.findById(tenantId, edgeId.getId());
        if (edge == null) {
            throw new DataValidationException("Can't unassign dashboard from non-existent edge!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete dashboard relation. Edge Id: [{}]", dashboardId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(dashboardId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return dashboard;
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public DashboardInfo findFirstDashboardInfoByTenantIdAndName(TenantId tenantId, String name) {
        return dashboardInfoDao.findFirstByTenantIdAndName(tenantId.getId(), name);
    }

    @Override
    public void saveUIByTenantId(TenantId tenantId, Map<String, String> params) {
        log.trace("Executing saveUIByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Tenant tenant = jpaTenantDao.findById(tenantId,tenantId.getId());
        if (tenant == null) {
            throw new DataValidationException("Can't save ui for non-existent tenant!");
        }
        ObjectNode additionalInfo = (ObjectNode) tenant.getAdditionalInfo();
        checkAndPutAdditionalInfo(additionalInfo, "applicationTitle", params);
        checkAndPutAdditionalInfo(additionalInfo, "iconImageUrl", params);
        checkAndPutAdditionalInfo(additionalInfo, "logoImageUrl", params);
        checkAndPutAdditionalInfo(additionalInfo, "logoImageHeight", params);
        checkAndPutAdditionalInfo(additionalInfo, "platformMainColor", params);
        checkAndPutAdditionalInfo(additionalInfo, "platformButtonColor", params);
        checkAndPutAdditionalInfo(additionalInfo, "showNameVersion", params);
        checkAndPutAdditionalInfo(additionalInfo, "platformName", params);
        checkAndPutAdditionalInfo(additionalInfo, "platformVersion", params);
        checkAndPutAdditionalInfo(additionalInfo, "platformTextMainColor", params);
        checkAndPutAdditionalInfo(additionalInfo, "platformMenuColorActive", params);
        checkAndPutAdditionalInfo(additionalInfo, "platformMenuColorHover", params);
        checkAndPutAdditionalInfo(additionalInfo, "iconsColor", params);
        checkAndPutAdditionalInfo(additionalInfo,"customCss", params);
        try {
            tenant.setAdditionalInfo(new ObjectMapper().readValue(additionalInfo.toString(), JsonNode.class));
            jpaTenantDao.save(tenantId, tenant);
        } catch (IOException e) {
            throw new IncorrectParameterException("Unable to save tenant ui.", e);
        }
    }

    private void checkAndPutAdditionalInfo(ObjectNode additionalInfo, String fieldName, Map<String, String> params) {
        if (params.get(fieldName) == null) {
            if (additionalInfo.has(fieldName)) {
                additionalInfo.remove(fieldName);
            }
        } else {
            additionalInfo.put(fieldName, params.get(fieldName));
        }
    }


    @Override
    public Map<String, Object> getTenantUIInfo(TenantId tenantId) {
        log.trace("Executing getTenantUIInfo, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Tenant tenant = jpaTenantDao.findById(tenantId,tenantId.getId());
        if (tenant == null) {
            throw new DataValidationException("Can't find ui for non-existent tenant!");
        }
        JsonNode additionalInfo = tenant.getAdditionalInfo();
        Map<String, Object> map = new HashMap<>();
        String applicationTitle = additionalInfo.get("applicationTitle") != null ? additionalInfo.get("applicationTitle").asText() : null;
        String iconImageUrl = additionalInfo.get("iconImageUrl") != null ? additionalInfo.get("iconImageUrl").asText() : null;
        String logoImageUrl = additionalInfo.get("logoImageUrl") != null ? additionalInfo.get("logoImageUrl").asText() : null;
        String logoImageHeight = additionalInfo.get("logoImageHeight") != null ? additionalInfo.get("logoImageHeight").asText() : null;
        String platformMainColor = additionalInfo.get("platformMainColor") != null ? additionalInfo.get("platformMainColor").asText() : null;
        String iconsColor = additionalInfo.get("iconsColor") != null ? additionalInfo.get("iconsColor").asText() : null;
        String customCss = additionalInfo.get("customCss") != null ? additionalInfo.get("customCss").asText() : null;
        String platformTextMainColor = additionalInfo.get("platformTextMainColor") != null ? additionalInfo.get("platformTextMainColor").asText() : null;
        String platformButtonColor = additionalInfo.get("platformButtonColor") != null ? additionalInfo.get("platformButtonColor").asText() : null;
        String platformMenuColorActive = additionalInfo.get("platformMenuColorActive") != null ? additionalInfo.get("platformMenuColorActive").asText() : null;
        String platformMenuColorHover = additionalInfo.get("platformMenuColorHover") != null ? additionalInfo.get("platformMenuColorHover").asText() : null;
        boolean showNameVersion = additionalInfo.get("showNameVersion") != null && additionalInfo.get("showNameVersion").asBoolean(false);
        String platformName = additionalInfo.get("platformName") != null ? additionalInfo.get("platformName").asText() : null;
        String platformVersion = additionalInfo.get("platformVersion") != null ? additionalInfo.get("platformVersion").asText() : null;
        map.put("applicationTitle", applicationTitle);
        map.put("iconImageUrl", iconImageUrl);
        map.put("logoImageUrl", logoImageUrl);
        map.put("logoImageHeight", logoImageHeight);
        map.put("platformMainColor", platformMainColor);
        map.put("platformTextMainColor", platformTextMainColor);
        map.put("platformButtonColor", platformButtonColor);
        map.put("platformMenuColorActive", platformMenuColorActive);
        map.put("platformMenuColorHover", platformMenuColorHover);
        map.put("showNameVersion", showNameVersion);
        map.put("platformName", platformName);
        map.put("platformVersion", platformVersion);
        map.put("iconsColor", iconsColor);
        map.put("customCss", customCss);
        return map;
    }

    @Override
public void saveLoginUIByTenantId(TenantId tenantId, Map<String, String> params) {
    log.trace("Executing saveLoginUIByTenantId, tenantId [{}]", tenantId);
    Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
    String domain = params.get("loginDomainName") + params.get("loginBaseUrl");
    TenantEntity tenantCheck = jpaTenantDao.findByDomain(domain);
    if (tenantCheck != null && !tenantCheck.getId().equals(tenantId.getId())) {
        throw new DataValidationException("The domain name already exists");
    }
    Tenant tenant = jpaTenantDao.findById(tenantId, tenantId.getId());
    if (tenant == null) {
        throw new DataValidationException("Can't save login ui for non-existent tenant!");
    }
    ObjectNode additionalInfo = (ObjectNode) tenant.getAdditionalInfo();
    checkAndPutAdditionalInfo(additionalInfo, "loginAppTitle", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginIconImageUrl", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginBGImage", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginLogoImageUrl", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginLogoImageHeight", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginBGColor", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginFormBGColor", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginFormTextColor", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginFormIconColor", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginFormInputColor", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginButtonColor", params);
    checkAndPutAdditionalInfo(additionalInfo, "loginButtonTextColor", params);
    try {
        tenant.setDomain(params.get("loginDomainName"));
        tenant.setAdditionalInfo(new ObjectMapper().readValue(additionalInfo.toString(), JsonNode.class));
        jpaTenantDao.save(tenantId, tenant);
    } catch (IOException e) {
        throw new IncorrectParameterException("Unable to save tenant login ui.", e);
    }
}

@Override
public Map<String, Object> getTenantLoginUIInfo(String domain, String tenantId) {
    log.trace("Executing getTenantLoginUIInfo, domain [{}]", domain);
    Map<String, Object> map = new HashMap<>();
    JsonNode additionalInfo = null;
    if (StringUtils.isNotBlank(tenantId)) {
        Tenant tenant = jpaTenantDao.findById(new TenantId(UUID.fromString(tenantId)), UUID.fromString(tenantId));
        map.put("loginDomainName", tenant.getDomain());
        additionalInfo = tenant.getAdditionalInfo();
    } else {
        TenantEntity tenantEntity = jpaTenantDao.findByDomain(domain);
        if (tenantEntity != null) {
            map.put("loginDomainName", tenantEntity.getDomain());
            additionalInfo = tenantEntity.getAdditionalInfo();
        }
    }
    if (additionalInfo != null) {
        String loginAppTitle = additionalInfo.get("loginAppTitle") != null ? additionalInfo.get("loginAppTitle").asText() : null;
        String loginIconImageUrl = additionalInfo.get("loginIconImageUrl") != null ? additionalInfo.get("loginIconImageUrl").asText() : null;
        String loginBGImage = additionalInfo.get("loginBGImage") != null ? additionalInfo.get("loginBGImage").asText() : null;
        String loginLogoImageUrl = additionalInfo.get("loginLogoImageUrl") != null ? additionalInfo.get("loginLogoImageUrl").asText() : null;
        String loginLogoImageHeight = additionalInfo.get("loginLogoImageHeight") != null ? additionalInfo.get("loginLogoImageHeight").asText() : null;
        String loginBGColor = additionalInfo.get("loginBGColor") != null ? additionalInfo.get("loginBGColor").asText() : null;
        String loginFormBGColor = additionalInfo.get("loginFormBGColor") != null ? additionalInfo.get("loginFormBGColor").asText() : null;
        String loginFormTextColor = additionalInfo.get("loginFormTextColor") != null ? additionalInfo.get("loginFormTextColor").asText() : null;
        String loginFormIconColor = additionalInfo.get("loginFormIconColor") != null ? additionalInfo.get("loginFormIconColor").asText() : null;
        String loginFormInputColor = additionalInfo.get("loginFormInputColor") != null ? additionalInfo.get("loginFormInputColor").asText() : null;
        String loginButtonColor = additionalInfo.get("loginButtonColor") != null ? additionalInfo.get("loginButtonColor").asText() : null;
        String loginButtonTextColor = additionalInfo.get("loginButtonTextColor") != null ? additionalInfo.get("loginButtonTextColor").asText() : null;
        map.put("loginAppTitle", loginAppTitle);
        map.put("loginIconImageUrl", loginIconImageUrl);
        map.put("loginBGImage", loginBGImage);
        map.put("loginLogoImageUrl", loginLogoImageUrl);
        map.put("loginLogoImageHeight", loginLogoImageHeight);
        map.put("loginBGColor", loginBGColor);
        map.put("loginFormBGColor", loginFormBGColor);
        map.put("loginFormTextColor", loginFormTextColor);
        map.put("loginFormIconColor", loginFormIconColor);
        map.put("loginFormInputColor", loginFormInputColor);
        map.put("loginButtonColor", loginButtonColor);
        map.put("loginButtonTextColor", loginButtonTextColor);
    }
    return map;
}

    @Override
    public List<Dashboard> findTenantDashboardsByTitle(TenantId tenantId, String title) {
        return dashboardDao.findByTenantIdAndTitle(tenantId.getId(), title);
    }

    private final PaginatedRemover<TenantId, DashboardId> tenantDashboardsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<DashboardId> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return dashboardDao.findIdsByTenantId(id, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, DashboardId dashboardId) {
            deleteDashboard(tenantId, dashboardId);
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDashboardById(tenantId, new DashboardId(entityId.getId())));
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return dashboardDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteDashboard(tenantId, (DashboardId) id);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

    private class CustomerDashboardsUnassigner extends PaginatedRemover<Customer, DashboardInfo> {

        private Customer customer;

        CustomerDashboardsUnassigner(Customer customer) {
            this.customer = customer;
        }

        @Override
        protected PageData<DashboardInfo> findEntities(TenantId tenantId, Customer customer, PageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(customer.getTenantId().getId(), customer.getId().getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
            unassignDashboardFromCustomer(customer.getTenantId(), new DashboardId(entity.getUuidId()), this.customer.getId());
        }

    }

    private class CustomerDashboardsUpdater extends PaginatedRemover<Customer, DashboardInfo> {

        private Customer customer;

        CustomerDashboardsUpdater(Customer customer) {
            this.customer = customer;
        }

        @Override
        protected PageData<DashboardInfo> findEntities(TenantId tenantId, Customer customer, PageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(customer.getTenantId().getId(), customer.getId().getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
            updateAssignedCustomer(customer.getTenantId(), new DashboardId(entity.getUuidId()), this.customer);
        }

    }

    

}
