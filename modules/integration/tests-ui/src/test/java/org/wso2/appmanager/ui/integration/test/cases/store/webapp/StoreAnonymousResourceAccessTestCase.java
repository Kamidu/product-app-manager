package org.wso2.appmanager.ui.integration.test.cases.store.webapp;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appmanager.ui.integration.test.dto.WebApp;
import org.wso2.appmanager.ui.integration.test.pages.LoginPage;
import org.wso2.appmanager.ui.integration.test.pages.PublisherCreateWebAppPage;
import org.wso2.appmanager.ui.integration.test.pages.PublisherWebAppsListPage;
import org.wso2.appmanager.ui.integration.test.pages.StoreHomePage;
import org.wso2.appmanager.ui.integration.test.utils.AppManagerIntegrationTest;
import org.wso2.appmanager.ui.integration.test.utils.AppmUiTestConstants;

public class StoreAnonymousResourceAccessTestCase extends AppManagerIntegrationTest {
    private static final String TEST_DESCRIPTION = "Verify Anonymous resource access";
    private static final String TEST_NON_ANONYMOUS_APP_NAME =
            "StoreAnonymousResourceAccess_non_anonymous_app";
    private static final String TEST_APP_VERSION = "1.0";
    private static final String TEST_APP_TRANSPORT = "http";

    private String testAppURL;

    private static final String STATE_SUBMIT = "Submit for Review";
    private static final String STATE_APPROVE = "Approve";
    private static final String STATE_PUBLISH = "Publish";

    private static final Log log = LogFactory.getLog(StoreAnonymousResourceAccessTestCase.class);

    private PublisherWebAppsListPage webAppsListPage;
    private PublisherCreateWebAppPage createWebAppPage;

    private String nonAnonymousAppId;
    WebDriverWait wait;

    @BeforeClass(alwaysRun = true)
    public void startUp() throws Exception {
        super.init();
        wait = new WebDriverWait(driver, 120);

        testAppURL = appMServer.getContextUrls().getWebAppURLHttps() + "/" +
                AppmUiTestConstants.SAMPLE_DEPLOYED_WEB_APP_NAME;

        //login to publisher
        webAppsListPage = (PublisherWebAppsListPage) login(driver, LoginPage.LoginTo.PUBLISHER);
        createWebAppPage = webAppsListPage.gotoCreateWebAppPage();

        //create and publish anonymous and non anonymous apps
        createAndPublishApps();
    }

    @Test(groups = TEST_GROUP, description = TEST_DESCRIPTION)
    public void testAnonymousApplicationAccess() throws Exception {
        //Access anonymous allowed web app resource using an anonymous user
        accessApps(true, AppmUiTestConstants.SAMPLE_DEPLOYED_WEB_APP_ANONYMOUS_RESOURCE);

        //Access anonymous disallowed web app resource using an anonymous user
        accessApps(false, AppmUiTestConstants.SAMPLE_DEPLOYED_WEB_APP_BLOCKED_RESOURCE);
    }


    private void createAndPublishApps() throws Exception {
        //create non anonymous app
        createWebAppPage = webAppsListPage.gotoCreateWebAppPage();

        webAppsListPage = createWebAppPage.createNonAnonymousAppWithAnonymousAndBlockedResources(
                new WebApp(
                        TEST_NON_ANONYMOUS_APP_NAME,
                        TEST_NON_ANONYMOUS_APP_NAME,
                        TEST_NON_ANONYMOUS_APP_NAME,
                        TEST_APP_VERSION, testAppURL,
                        TEST_APP_TRANSPORT),
                AppmUiTestConstants.SAMPLE_DEPLOYED_WEB_APP_ANONYMOUS_RESOURCE,
                AppmUiTestConstants.SAMPLE_DEPLOYED_WEB_APP_BLOCKED_RESOURCE);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                "[data-name='" + TEST_NON_ANONYMOUS_APP_NAME +
                        "'][data-action='" + STATE_SUBMIT + "']")));

        //Set app id
        nonAnonymousAppId = driver.findElement(By.cssSelector(
                "[data-name='" + TEST_NON_ANONYMOUS_APP_NAME +
                        "'][data-action='" + STATE_SUBMIT + "']"))
                .getAttribute("data-app");

        changeLifeCycleState(TEST_NON_ANONYMOUS_APP_NAME, STATE_SUBMIT);
        changeLifeCycleState(TEST_NON_ANONYMOUS_APP_NAME, STATE_APPROVE);
        changeLifeCycleState(TEST_NON_ANONYMOUS_APP_NAME, STATE_PUBLISH);
    }


    private void changeLifeCycleState(String webAppName, String Status) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                "[data-name='" + webAppName +
                        "'][data-action='" + Status + "']")));
        driver.findElement(By.cssSelector(
                "[data-name='" + webAppName + "'][data-action='" + Status + "']"))
                .click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                "[data-dismiss='modal']")));
        driver.findElement(By.cssSelector("[data-dismiss='modal']")).click();
    }

    private void accessApps(Boolean isAnonymousAllowedResource, String resourceName)
            throws Exception {
        String redirectedURL;
        String exceptionMsg;

        //temp wait till the assets load, if any (repeat this 2 times to give a reasonable
        // waiting time)
        WebDriverWait tempWait = new WebDriverWait(driver, 5);
        for (int i = 0; i < 2; i++) {
            try {
                tempWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                        "a[href*='/store/assets/webapp/" + nonAnonymousAppId + "']")));
                driver.navigate().refresh();
                break;
            } catch (org.openqa.selenium.TimeoutException e) {
                //Expected error when no element found
            }
        }

        driver.get(appMServer.getContextUrls().getWebAppURLHttps() + "/store");

        if (isAnonymousAllowedResource) {
            redirectedURL = TEST_NON_ANONYMOUS_APP_NAME;
            exceptionMsg = "Anonymous App URL is invalid";
        } else {
            redirectedURL = AppmUiTestConstants.UNAUTHORIZED_LOGIN_REDIRECT_PAGE;
            exceptionMsg = "Non Anonymous Apps do not get redirected to login page";
        }

        StoreHomePage.getPage(driver, appMServer);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                "a[href*='/store/assets/webapp/" + nonAnonymousAppId + "']")));
        driver.findElement(By.cssSelector(
                "a[href*='/store/assets/webapp/" + nonAnonymousAppId + "']")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("gatewayURL")));
        driver.get(driver.findElement(By.id("gatewayURL")).getText() + resourceName);

        if (driver.getCurrentUrl().contains(redirectedURL) == false) {
            throw new Exception(exceptionMsg);
        }
    }


    @AfterClass(alwaysRun = true)
    public void closeDown() throws Exception {
        //Go to publisher listing page
        driver.get(appMServer.getContextUrls().getWebAppURLHttps() + "/publisher");
        PublisherWebAppsListPage.getPage(driver, appMServer);
        //Delete apps
        webAppsListPage.deleteApp(TEST_NON_ANONYMOUS_APP_NAME,
                                  appMServer.getSuperTenant().getTenantAdmin().getUserName(),
                                  TEST_APP_VERSION, driver);

        closeDriver(driver);
    }
}
