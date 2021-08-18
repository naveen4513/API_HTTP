package com.sirionlabs.test.internationalization;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public class fieldLabels extends TestDisputeInternationalization {
    public static WebDriver driver;
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(fieldLabels.class);
    /*private static String languageName = "Ukrainian (Українська)";*/
    private static String clientAlias = "_Ukrainiangood";

    @Test
    public void FieldLabelsTest () throws InterruptedException {


        System.setProperty("webdriver.chrome.driver", "D:\\java-api-framework\\Drivers\\chromedriver.exe");

        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().deleteAllCookies();
        driver.get("http://qa.pp.office");
        driver.findElement(By.name("username_login")).sendKeys("naveen_admin");
        driver.findElement(By.name("password_login")).sendKeys("admin123");

        driver.findElement(By.id("loginButton")).click();

        Thread.sleep(5000);

        //logger.debug("Executing Test Field Labels Test for -- "+languageName);

        //driver.get("clientAdminURL");
        Thread.sleep(5000);
        fluentWaitMethod(driver.findElement(By.linkText("Field Labels")));

        driver.findElement(By.linkText("Field Labels")).click();


        List<WebElement> list = driver.findElement(By.id("supplier")).findElements(By.tagName("option"));

        for (WebElement e : list) {

            if (e.getAttribute("value").equalsIgnoreCase("") || e.getAttribute("value").equalsIgnoreCase("1079") || e.getAttribute("value").equalsIgnoreCase("1083"))
                continue;

            Thread.sleep(5000);
            fluentWaitMethod(driver.findElement(By.id("language")));

            //new Select(driver.findElement(By.id("language"))).selectByVisibleText(languageName);
            Thread.sleep(5000);

            logger.debug("Executing Field Renaming for -- "+e.getText());
            System.out.println("Executing Field Renaming for -- "+e.getText());
            new Select(driver.findElement(By.id("supplier"))).selectByVisibleText(e.getText());

            Thread.sleep(5000);


            List<WebElement> optionsList = driver.findElement(By.className("rendr-part")).findElements(By.tagName("input"));
            for (WebElement element : optionsList) {
                if (element.getAttribute("id").contains("cnt")) {
                    WebElement elementParent = element.findElement(By.xpath("..")).findElement(By.xpath(".."));


                    if (elementParent.getText().contains(">")){

                        element.sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END), elementParent.getText().replace(">","Greater Than").trim() + clientAlias);
                    }else if (elementParent.getText().contains("<")){

                        element.sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END), elementParent.getText().replace("<","Less Than").trim() + clientAlias);
                    }

                    element.sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END), elementParent.getText().trim() + clientAlias);
                    //element.sendKeys(elementParent.getText().trim() + clientAlias);
                }
            }

            driver.findElement(By.id("updateBtn")).click();


            driver.findElement(By.id("alertdialog"));
            String entityAlert = driver.findElement(By.id("alertdialog")).getText();

            System.out.println("" +entityAlert);

            //	Assert.assertEquals(entityAlert, "Successfully updated");

            driver.findElement(By.cssSelector("button.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only")).click();

            Thread.sleep(5000);
        }

        //ogger.debug("Execution of Field Labels Test for -- "+languageName + " -- is PASSED");
        driver.get("clientAdminURL");
    }

    public static void fluentWaitMethod(WebElement Element){

        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);

        Function<WebDriver, Boolean> function = arg0 -> {
            WebElement element = Element;
            if (element.isDisplayed()) {
                return true;
            }
            return false;
        };

        wait.until(function);
    }


}