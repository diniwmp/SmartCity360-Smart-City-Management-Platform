package com.urbantech.smartcity360.listener;

import com.urbantech.smartcity360.service.auth.DemoUserSeeder;
import com.urbantech.smartcity360.service.complaint.DemoComplaintSeeder;
import com.urbantech.smartcity360.service.energy.DemoEnergySeeder;
import com.urbantech.smartcity360.service.lighting.DemoStreetlightSeeder;
import com.urbantech.smartcity360.service.notification.AlertMonitor;
import com.urbantech.smartcity360.service.notification.DemoNotificationSeeder;
import com.urbantech.smartcity360.service.policy.DemoPolicySeeder;
import com.urbantech.smartcity360.service.sensor.DemoSensorSeeder;
import com.urbantech.smartcity360.service.waste.DemoWasteSeeder;
import com.urbantech.smartcity360.service.zone.DemoZoneSeeder;
import com.urbantech.smartcity360.util.HibernateUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartupListener.class.getName());

    private final AlertMonitor alertMonitor = new AlertMonitor();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        HibernateUtil.getSessionFactory();


        runSeeder("users", () -> new DemoUserSeeder().seedIfEmpty());
        runSeeder("zones", () -> new DemoZoneSeeder().seedIfEmpty());
        runSeeder("energy", () -> new DemoEnergySeeder().seedIfEmpty());
        runSeeder("street lights", () -> new DemoStreetlightSeeder().seedIfEmpty());
        runSeeder("sensors", () -> new DemoSensorSeeder().seedIfEmpty());
        runSeeder("waste", () -> new DemoWasteSeeder().seedIfEmpty());
        runSeeder("complaints", () -> new DemoComplaintSeeder().seedIfEmpty());
        runSeeder("policies", () -> new DemoPolicySeeder().seedIfEmpty());
        runSeeder("notifications", () -> new DemoNotificationSeeder().seedIfEmpty());

        alertMonitor.start();
        LOG.info("SmartCity360 started.");
    }

    private void runSeeder(String name, Runnable seeder) {
        try {
            seeder.run();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Demo data seeding failed for: " + name, e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        alertMonitor.stop();
        HibernateUtil.shutdown();
    }
}