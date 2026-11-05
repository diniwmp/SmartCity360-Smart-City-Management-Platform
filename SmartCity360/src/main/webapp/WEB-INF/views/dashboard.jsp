<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/sidebar.jsp"/>

<div class="welcome-banner">
    <h4 class="fw-bold mb-1">Welcome, <c:out value="${sessionScope.loggedUser.fullName}"/> 👋</h4>
    <p class="mb-2 opacity-75">SmartCity360 control centre</p>
    <span class="role-badge">${sessionScope.loggedUser.roleLabel}</span>
</div>

<c:if test="${not empty dashboard}">

    <div class="row g-3 mb-4">
        <div class="col-6 col-xl-3">
            <a href="${ctx}/complaints" class="metric-card metric-link">
                <div class="metric-icon amber"><i class="bi bi-chat-left-text"></i></div>
                <div>
                    <div class="metric-value">${dashboard.complaintStats.open}</div>
                    <div class="metric-label">Open complaints</div>
                </div>
            </a>
        </div>
        <div class="col-6 col-xl-3">
            <a href="${ctx}/notifications" class="metric-card metric-link">
                <div class="metric-icon purple"><i class="bi bi-bell"></i></div>
                <div>
                    <div class="metric-value">${dashboard.notificationOverview.unread}</div>
                    <div class="metric-label">Unread alerts
                        <c:if test="${dashboard.notificationOverview.criticalUnread > 0}">
                            <span class="text-danger fw-semibold">(${dashboard.notificationOverview.criticalUnread} critical)</span>
                        </c:if>
                    </div>
                </div>
            </a>
        </div>

        <c:if test="${dashboard.cityWide}">
            <div class="col-6 col-xl-3">
                <a href="${ctx}/energy" class="metric-card metric-link">
                    <div class="metric-icon teal"><i class="bi bi-lightning-charge"></i></div>
                    <div>
                        <div class="metric-value"><fmt:formatNumber value="${dashboard.energyOverview.totalKwh}" maxFractionDigits="1"/>
                            <span class="fs-6 text-muted fw-normal">kWh</span></div>
                        <div class="metric-label">Energy recorded</div>
                    </div>
                </a>
            </div>
            <div class="col-6 col-xl-3">
                <a href="${ctx}/waste" class="metric-card metric-link">
                    <div class="metric-icon ${dashboard.wasteOverview.overflowing > 0 ? 'red' : 'blue'}"><i class="bi bi-recycle"></i></div>
                    <div>
                        <div class="metric-value">${dashboard.wasteOverview.needsCollection}</div>
                        <div class="metric-label">Bins need collection</div>
                    </div>
                </a>
            </div>
            <div class="col-6 col-xl-3">
                <a href="${ctx}/streetlights" class="metric-card metric-link">
                    <div class="metric-icon amber"><i class="bi bi-lightbulb"></i></div>
                    <div>
                        <div class="metric-value">${dashboard.lightOverview.lit}<span class="fs-6 text-muted fw-normal"> / ${dashboard.lightOverview.total}</span></div>
                        <div class="metric-label">Street lights lit</div>
                    </div>
                </a>
            </div>
            <div class="col-6 col-xl-3">
                <a href="${ctx}/sensors" class="metric-card metric-link">
                    <div class="metric-icon blue"><i class="bi bi-broadcast"></i></div>
                    <div>
                        <div class="metric-value">${dashboard.sensorOverview.activeSensors}<span class="fs-6 text-muted fw-normal"> / ${dashboard.sensorOverview.totalSensors}</span></div>
                        <div class="metric-label">Sensors active</div>
                    </div>
                </a>
            </div>
            <div class="col-6 col-xl-3">
                <a href="${ctx}/energy" class="metric-card metric-link">
                    <div class="metric-icon purple"><i class="bi bi-shield-lock"></i></div>
                    <div>
                        <div class="metric-value">${dashboard.energyOverview.securedPercent}<span class="fs-6 text-muted fw-normal">%</span></div>
                        <div class="metric-label">Readings secured</div>
                    </div>
                </a>
            </div>
        </c:if>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-lg-6 col-xl-4">
            <div class="card h-100"><div class="card-body">
                <h6 class="fw-bold mb-3"><i class="bi bi-exclamation-triangle text-teal"></i> Complaints by severity</h6>
                <div class="chart-box"><canvas id="complaintSeverityChart"></canvas></div>
                <div id="complaintSeverityData" hidden
                     data-labels="<c:forEach var="p" items="${dashboard.complaintsBySeverity}" varStatus="s">${p.label}<c:if test="${!s.last}">|</c:if></c:forEach>"
                     data-values="<c:forEach var="p" items="${dashboard.complaintsBySeverity}" varStatus="s">${p.value}<c:if test="${!s.last}">|</c:if></c:forEach>"
                     data-colors="<c:forEach var="p" items="${dashboard.complaintsBySeverity}" varStatus="s">${p.color}<c:if test="${!s.last}">|</c:if></c:forEach>"></div>
            </div></div>
        </div>
        <div class="col-lg-6 col-xl-4">
            <div class="card h-100"><div class="card-body">
                <h6 class="fw-bold mb-3"><i class="bi bi-diagram-2 text-teal"></i> Complaints by status</h6>
                <div class="chart-box"><canvas id="complaintStatusChart"></canvas></div>
                <div id="complaintStatusData" hidden
                     data-labels="<c:forEach var="p" items="${dashboard.complaintsByStatus}" varStatus="s">${p.label}<c:if test="${!s.last}">|</c:if></c:forEach>"
                     data-values="<c:forEach var="p" items="${dashboard.complaintsByStatus}" varStatus="s">${p.value}<c:if test="${!s.last}">|</c:if></c:forEach>"
                     data-colors="<c:forEach var="p" items="${dashboard.complaintsByStatus}" varStatus="s">${p.color}<c:if test="${!s.last}">|</c:if></c:forEach>"></div>
            </div></div>
        </div>

        <c:if test="${dashboard.cityWide}">
            <div class="col-lg-6 col-xl-4">
                <div class="card h-100"><div class="card-body">
                    <h6 class="fw-bold mb-3"><i class="bi bi-lightbulb text-teal"></i> Street lights</h6>
                    <div class="chart-box"><canvas id="lightsChart"></canvas></div>
                    <div id="lightsData" hidden
                         data-labels="<c:forEach var="p" items="${dashboard.lightsByStatus}" varStatus="s">${p.label}<c:if test="${!s.last}">|</c:if></c:forEach>"
                         data-values="<c:forEach var="p" items="${dashboard.lightsByStatus}" varStatus="s">${p.value}<c:if test="${!s.last}">|</c:if></c:forEach>"
                         data-colors="<c:forEach var="p" items="${dashboard.lightsByStatus}" varStatus="s">${p.color}<c:if test="${!s.last}">|</c:if></c:forEach>"></div>
                </div></div>
            </div>
            <div class="col-lg-6 col-xl-4">
                <div class="card h-100"><div class="card-body">
                    <h6 class="fw-bold mb-3"><i class="bi bi-recycle text-teal"></i> Waste bins</h6>
                    <div class="chart-box"><canvas id="binsChart"></canvas></div>
                    <div id="binsData" hidden
                         data-labels="<c:forEach var="p" items="${dashboard.binsByStatus}" varStatus="s">${p.label}<c:if test="${!s.last}">|</c:if></c:forEach>"
                         data-values="<c:forEach var="p" items="${dashboard.binsByStatus}" varStatus="s">${p.value}<c:if test="${!s.last}">|</c:if></c:forEach>"
                         data-colors="<c:forEach var="p" items="${dashboard.binsByStatus}" varStatus="s">${p.color}<c:if test="${!s.last}">|</c:if></c:forEach>"></div>
                </div></div>
            </div>
            <div class="col-xl-8">
                <div class="card h-100"><div class="card-body">
                    <h6 class="fw-bold mb-3"><i class="bi bi-bar-chart text-teal"></i> Energy used, top meters (kWh)</h6>
                    <div class="chart-box"><canvas id="energyChart"></canvas></div>
                    <div id="energyData" hidden
                         data-labels="<c:forEach var="p" items="${dashboard.energyByMeter}" varStatus="s"><c:out value="${p.label}"/><c:if test="${!s.last}">|</c:if></c:forEach>"
                         data-values="<c:forEach var="p" items="${dashboard.energyByMeter}" varStatus="s">${p.value}<c:if test="${!s.last}">|</c:if></c:forEach>"
                         data-colors="<c:forEach var="p" items="${dashboard.energyByMeter}" varStatus="s">${p.color}<c:if test="${!s.last}">|</c:if></c:forEach>"></div>
                </div></div>
            </div>
        </c:if>
    </div>
</c:if>

<h6 class="text-uppercase text-muted fw-bold mb-3" style="letter-spacing:.05em;">Your modules</h6>
<div class="row g-3">
    <c:forEach var="m" items="${sessionScope.menuModules}">
        <c:if test="${m.path ne '/dashboard'}">
            <div class="col-12 col-md-6 col-xl-4">
                <div class="module-card ${m.available ? '' : 'disabled'}">
                    <div class="module-icon"><i class="bi ${m.icon}"></i></div>
                    <div>
                        <h6>${m.label}</h6>
                        <c:choose>
                            <c:when test="${m.available}">
                                <a href="${ctx}${m.path}" class="stretched-link small text-decoration-none">Open module &rarr;</a>
                            </c:when>
                            <c:otherwise>
                                <span class="badge text-bg-secondary">Coming soon</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </c:if>
    </c:forEach>
</div>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.3/dist/chart.umd.min.js"></script>
<script src="${ctx}/assets/js/dashboard.js"></script>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>