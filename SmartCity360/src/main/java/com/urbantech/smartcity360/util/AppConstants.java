package com.urbantech.smartcity360.util;

import java.util.List;
import java.util.Set;

//All constants are defined here to avoid magic strings and numbers
public final class AppConstants {

    private AppConstants() { }

    // Session / request attribute names
    public static final String SESSION_USER = "loggedUser";
    public static final String SESSION_MENU = "menuModules";
    public static final String ATTR_ERROR = "errorMessage";
    public static final String ATTR_ERRORS = "errors";
    public static final String ATTR_FORM = "form";
    public static final String ATTR_PAGE_TITLE = "pageTitle";
    public static final String ATTR_ENTERED_USERNAME = "enteredUsername";

    // URL paths
    public static final String PATH_ROOT = "/";
    public static final String PATH_LOGIN_PAGE = "/index.jsp";
    public static final String PATH_LOGIN = "/login";
    public static final String PATH_LOGOUT = "/logout";
    public static final String PATH_REGISTER = "/register";
    public static final String PATH_DASHBOARD = "/dashboard";
    public static final String ASSETS_PREFIX = "/assets/";

    // Views (MVC: Controller → View)
    public static final String VIEW_DASHBOARD = "/WEB-INF/views/dashboard.jsp";
    public static final String VIEW_REGISTER = "/WEB-INF/views/register.jsp";
    public static final String VIEW_ACCESS_DENIED = "/WEB-INF/views/error/403.jsp";

    // Smart Zones (Module 2)
    public static final String PATH_ZONES = "/zones";
    public static final String VIEW_ZONES = "/WEB-INF/views/zone-hierarchy.jsp";
    public static final String ATTR_ZONE_ROWS = "zoneRows";
    public static final String ATTR_ZONE_REPORT = "zoneReport";
    public static final String ATTR_CAN_MANAGE_ZONES = "canManageZones";

    // Flash messages
    public static final String FLASH_SUCCESS = "flashSuccess";
    public static final String FLASH_ERROR = "flashError";

    // Session
    public static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    // Energy Monitoring
    public static final String PATH_ENERGY = "/energy";
    public static final String VIEW_ENERGY = "/WEB-INF/views/energy-stream.jsp";

    public static final String ATTR_ENERGY_OVERVIEW = "overview";
    public static final String ATTR_METER_ROWS = "meterRows";
    public static final String ATTR_READING_ROWS = "readingRows";
    public static final String ATTR_BUILDING_ZONES = "buildingZones";
    public static final String ATTR_SECURITY_LAYERS = "securityLayers";
    public static final String ATTR_SELECTED_LAYERS = "selectedLayers";
    public static final String ATTR_INSPECTION = "inspection";
    public static final String ATTR_FILTER_METER = "filterMeterId";
    public static final String ATTR_CAN_MANAGE_METERS = "canManageMeters";
    public static final String ATTR_SEND_FORM = "sendForm";
    public static final String ATTR_SEND_ERRORS = "sendErrors";
    public static final String ATTR_METER_FORM = "meterForm";
    public static final String ATTR_METER_ERRORS = "meterErrors";

    public static final int READING_HISTORY_LIMIT = 20;
    public static final double MAX_KWH_PER_READING = 10_000;
    public static final double MIN_VOLTAGE = 100;
    public static final double MAX_VOLTAGE = 500;
    public static final double DEFAULT_VOLTAGE = 230;
    public static final int MAX_DECOMPRESSED_BYTES = 1_048_576;
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    // Complaints
    public static final String PATH_COMPLAINTS = "/complaints";
    public static final String VIEW_COMPLAINTS = "/WEB-INF/views/complaints.jsp";

    public static final String ATTR_COMPLAINT_ROWS = "complaintRows";
    public static final String ATTR_COMPLAINT_STATS = "complaintStats";
    public static final String ATTR_COMPLAINT_DETAIL = "detail";
    public static final String ATTR_COMPLAINT_FORM = "complaintForm";
    public static final String ATTR_COMPLAINT_ERRORS = "complaintErrors";
    public static final String ATTR_STATUS_FILTER = "statusFilter";
    public static final String ATTR_CAN_SUBMIT = "canSubmitComplaint";
    public static final String ATTR_SEVERITY_OPTIONS = "severityOptions";
    public static final String ATTR_CATEGORIES = "categories";
    public static final String ATTR_STREET_ZONES = "streetZones";
    public static final String ATTR_ESCALATION_LEVELS = "escalationLevels";
    public static final String ATTR_COMPLAINT_STATUSES = "complaintStatuses";

    public static final String COMPLAINT_REFERENCE_PREFIX = "CMP";
    public static final String COMPLAINT_FILTER_OPEN = "OPEN";
    public static final int COMPLAINT_TITLE_MIN = 5;
    public static final int COMPLAINT_TITLE_MAX = 150;
    public static final int COMPLAINT_DESCRIPTION_MIN = 10;
    public static final int COMPLAINT_DESCRIPTION_MAX = 2000;
    public static final int COMPLAINT_NOTE_MIN = 5;
    public static final int COMPLAINT_NOTE_MAX = 255;


    // Street Lights
    public static final String PATH_STREETLIGHTS = "/streetlights";
    public static final String VIEW_STREETLIGHTS = "/WEB-INF/views/streetlights.jsp";
    public static final String SESSION_MEMORY_RESULT = "lastMemoryComparison";

    public static final String ATTR_LIGHT_OVERVIEW = "lightOverview";
    public static final String ATTR_LIGHT_PAGE = "lightPage";
    public static final String ATTR_SPEC_ROWS = "specRows";
    public static final String ATTR_POOL_STATS = "poolStats";
    public static final String ATTR_MEMORY_RESULT = "memoryResult";
    public static final String ATTR_SCALING_ROWS = "scalingRows";
    public static final String ATTR_LIGHT_STATUSES = "lightStatuses";
    public static final String ATTR_INSTALL_OPTIONS = "installOptions";
    public static final String ATTR_MEMORY_OPTIONS = "memoryOptions";
    public static final String ATTR_CAN_MANAGE_LIGHTS = "canManageLights";

    public static final int STREETLIGHT_PAGE_SIZE = 15;
    public static final int DEMO_LIGHT_COUNT = 5_000;
    public static final List<Integer> LIGHT_INSTALL_OPTIONS = List.of(1_000, 5_000, 10_000, 50_000);
    public static final List<Integer> MEMORY_TEST_OPTIONS = List.of(1_000, 10_000, 50_000, 100_000);

    // City Policies
    public static final String PATH_POLICIES = "/policies";
    public static final String VIEW_POLICIES = "/WEB-INF/views/policies.jsp";

    public static final String ATTR_POLICY_ROWS = "policyRows";
    public static final String ATTR_POLICY_FORM = "policyForm";
    public static final String ATTR_POLICY_ERRORS = "policyErrors";
    public static final String ATTR_POLICY_CATEGORIES = "policyCategories";
    public static final String ATTR_POLICY_VARIABLES = "policyVariables";
    public static final String ATTR_OPEN_POLICY_MODAL = "openPolicyModal";
    public static final String ATTR_TEST_FORM = "testForm";
    public static final String ATTR_TEST_ERRORS = "testErrors";
    public static final String ATTR_TEST_RESULT = "testResult";
    public static final String ATTR_CHECK_ROWS = "checkRows";

    public static final int POLICY_NAME_MIN = 3;
    public static final int POLICY_NAME_MAX = 120;
    public static final int POLICY_RULE_MAX_LENGTH = 500;
    public static final int POLICY_MAX_NESTING = 20;
    public static final String DEFAULT_TEST_TIME = "23:00";
    public static final String DEFAULT_TEST_RULE = "NOISE > 70 AND TIME BETWEEN 22:00 AND 06:00";

    // Sensor Network
    public static final String PATH_SENSORS = "/sensors";
    public static final String VIEW_SENSORS = "/WEB-INF/views/sensor-network.jsp";
    public static final String SESSION_SIMULATION = "lastSimulation";

    public static final String ATTR_SENSOR_OVERVIEW = "sensorOverview";
    public static final String ATTR_SENSOR_ROWS = "sensorRows";
    public static final String ATTR_SENSOR_READINGS = "sensorReadings";
    public static final String ATTR_SIMULATION = "simulation";
    public static final String ATTR_SENSOR_FORM = "sensorForm";
    public static final String ATTR_SENSOR_ERRORS = "sensorErrors";
    public static final String ATTR_SENSOR_TYPES = "sensorTypes";
    public static final String ATTR_SENSOR_PROTOCOLS = "sensorProtocols";
    public static final String ATTR_CAN_MANAGE_SENSORS = "canManageSensors";
    public static final String ATTR_FILTER_SENSOR = "filterSensorId";
    public static final String ATTR_ONLY_BREACHES = "onlyBreaches";

    public static final int SENSOR_READING_LIMIT = 15;
    public static final int SENSOR_READING_SCAN_LIMIT = 300;

    // Waste Management
    public static final String PATH_WASTE = "/waste";
    public static final String VIEW_WASTE = "/WEB-INF/views/waste-management.jsp";

    public static final String ATTR_BIN_OVERVIEW = "binOverview";
    public static final String ATTR_BIN_PAGE = "binPage";
    public static final String ATTR_ROUTE_ROWS = "routeRows";
    public static final String ATTR_BIN_TEMPLATES = "binTemplates";
    public static final String ATTR_BIN_STATUSES = "binStatuses";
    public static final String ATTR_ADD_BINS_FORM = "addBinsForm";
    public static final String ATTR_ADD_BINS_ERRORS = "addBinsErrors";
    public static final String ATTR_CAN_MANAGE_WASTE = "canManageWaste";

    public static final String WASTE_FILTER_NEEDS = "NEEDS";
    public static final int WASTE_PAGE_SIZE = 15;
    public static final int WASTE_FILL_STEP_MAX = 30;
    public static final int WASTE_MAX_BINS_PER_ADD = 50;
    public static final int WASTE_ROUTES_SHOWN = 10;

    // Stream security key
    public static final String ENV_STREAM_SECRET = "SMARTCITY360_STREAM_SECRET";
    public static final String DEFAULT_STREAM_SECRET = "SmartCity360-UrbanTech-Demo-Secret";

    // Registration validation rules
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MIN_USERNAME_LENGTH = 4;
    public static final int MAX_USERNAME_LENGTH = 30;
    public static final int MAX_EMAIL_LENGTH = 120;
    public static final int MIN_PASSWORD_LENGTH = 6;

    private static final Set<String> PUBLIC_PATHS =
            Set.of("", PATH_ROOT, PATH_LOGIN_PAGE, PATH_LOGIN, PATH_LOGOUT, PATH_REGISTER);

    public static boolean isPublicPath(String servletPath) {
        return PUBLIC_PATHS.contains(servletPath) || servletPath.startsWith(ASSETS_PREFIX);
    }
}