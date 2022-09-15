package com.octopus.decorators;

import com.octopus.AutomatedBrowser;
import com.octopus.Constants;
import com.octopus.decoratorbase.AutomatedBrowserBase;
import com.octopus.exceptions.WebElementException;
import com.octopus.utils.ExpectedConditionCallback;
import com.octopus.utils.MouseMovementUtils;
import com.octopus.utils.RetryService;
import com.octopus.utils.SimpleBy;
import com.octopus.utils.SystemPropertyUtils;
import com.octopus.utils.impl.MouseMovementUtilsImpl;
import com.octopus.utils.impl.RetryServiceImpl;
import com.octopus.utils.impl.SimpleByImpl;
import com.octopus.utils.impl.SystemPropertyUtilsImpl;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.retry.RetryCallback;

/**
 * This decorator wraps up commands that emulate end user interactions, and move the mouse to the element
 * location. The broser must be maximized or in full screen mode for mouse movement to work correctly, as
 * WebDriver can not find the absolute position of an element on the screen, but that position can be worked out
 * from the element position in the browser window plus a fixed value for the browsers user interface.
 */
public class MouseMovementDecorator extends AutomatedBrowserBase {
    /**
     * The shared MouseMovementUtilsImpl instance.
     */
    private static final MouseMovementUtils MOUSE_MOVEMENT_UTILS = new MouseMovementUtilsImpl();
    /**
     * The shared SystemPropertyUtilsImpl instance.
     */
    private static final SystemPropertyUtils SYSTEM_PROPERTY_UTILS = new SystemPropertyUtilsImpl();
    /**
     * The shared SimpleByImpl instance.
     */
    private static final SimpleBy SIMPLE_BY = new SimpleByImpl();
    /**
     * The shared RetryServiceImpl instance.
     */
    private static final RetryService RETRY_SERVICE = new RetryServiceImpl();
    /**
     * A count of how many user interactions we simulated.
     */
    private int interactionCount = 0;

    private void glideMouse(
        final String locator,
        final int waitTime,
        final ExpectedConditionCallback expectedConditionCallback) {
        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> SIMPLE_BY.getElement(
                getWebDriver(),
                locator,
                waitTime,
                expectedConditionCallback),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);
    }

    private WebElement getElementById(final String id, final int waitTime) {
        if (waitTime <= 0) {
            return getWebDriver().findElement(By.id(id));
        } else {
            final WebDriverWait wait = new WebDriverWait(getWebDriver(), Duration.ofSeconds(waitTime));
            return wait.until(ExpectedConditions.elementToBeClickable((By.id(id))));
        }
    }

    private WebElement getElementByXPath(final String xpath, final int waitTime) {
        if (waitTime <= 0) {
            return getWebDriver().findElement(By.xpath(xpath));
        } else {
            final WebDriverWait wait = new WebDriverWait(getWebDriver(), Duration.ofSeconds(waitTime));
            return wait.until(ExpectedConditions.elementToBeClickable((By.xpath(xpath))));
        }
    }

    private WebElement getElementByCSSSelector(final String css, final int waitTime) {
        if (waitTime <= 0) {
            return getWebDriver().findElement(By.cssSelector(css));
        } else {
            final WebDriverWait wait = new WebDriverWait(getWebDriver(), Duration.ofSeconds(waitTime));
            return wait.until(ExpectedConditions.elementToBeClickable((By.cssSelector(css))));
        }
    }

    private WebElement getElementByName(final String name, final int waitTime) {
        if (waitTime <= 0) {
            return getWebDriver().findElement(By.name(name));
        } else {
            final WebDriverWait wait = new WebDriverWait(getWebDriver(), Duration.ofSeconds(waitTime));
            return wait.until(ExpectedConditions.elementToBeClickable((By.name(name))));
        }
    }

    /**
     * Default constructor.
     */
    public MouseMovementDecorator() {
        super(null);
    }

    /**
     * Decorator constructor.
     *
     * @param automatedBrowser The AutomatedBrowser to wrap up.
     */
    public MouseMovementDecorator(final AutomatedBrowser automatedBrowser) {
        super(automatedBrowser);
    }

    @Override
    public void clickElementWithId(final String id) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementById(id, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithId(
                id,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void clickElementWithId(final String id, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementById(id, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithId(
                id,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void selectOptionByTextFromSelectWithId(final String optionText, final String id) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementById(id, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithId(
                optionText,
                id,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void selectOptionByTextFromSelectWithId(final String optionText, final String id, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementById(id, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithId(
                optionText,
                id,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void populateElementWithId(final String id, final String text) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementById(id, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithId(
                id,
                text,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void populateElementWithId(final String id, final String text, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementById(id, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithId(
                id,
                text,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void clickElementWithXPath(final String xpath) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByXPath(xpath, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithXPath(
                xpath,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void clickElementWithXPath(final String xpath, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByXPath(xpath, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithXPath(
                xpath,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void selectOptionByTextFromSelectWithXPath(final String optionText, final String xpath) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByXPath(xpath, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithXPath(
                optionText,
                xpath,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void selectOptionByTextFromSelectWithXPath(final String optionText, final String xpath, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByXPath(xpath, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithXPath(
                optionText,
                xpath,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void populateElementWithXPath(final String xpath, final String text) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByXPath(xpath, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithXPath(
                xpath,
                text,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void populateElementWithXPath(final String xpath, final String text, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByXPath(xpath, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithXPath(
                xpath,
                text,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void clickElementWithCSSSelector(final String cssSelector) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByCSSSelector(cssSelector, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithCSSSelector(
                cssSelector,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void clickElementWithCSSSelector(final String cssSelector, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByCSSSelector(cssSelector, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithCSSSelector(
                cssSelector,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void selectOptionByTextFromSelectWithCSSSelector(final String optionText, final String cssSelector) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByCSSSelector(cssSelector, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithCSSSelector(
                optionText,
                cssSelector,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void selectOptionByTextFromSelectWithCSSSelector(final String optionText, final String cssSelector, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByCSSSelector(cssSelector, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithCSSSelector(
                optionText,
                cssSelector,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void populateElementWithCSSSelector(final String cssSelector, final String text) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByCSSSelector(cssSelector, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithCSSSelector(
                cssSelector,
                text,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void populateElementWithCSSSelector(final String cssSelector, final String text, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByCSSSelector(cssSelector, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithCSSSelector(
                cssSelector,
                text,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void clickElementWithName(final String name) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByName(name, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithName(
                name,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void clickElementWithName(final String name, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByName(name, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithName(
                name,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void selectOptionByTextFromSelectWithName(final String optionText, final String name) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByName(name, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithName(
                optionText,
                name,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void selectOptionByTextFromSelectWithName(final String optionText, final String name, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByName(name, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithName(
                optionText,
                name,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void populateElementWithName(final String name, final String text) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByName(name, getDefaultExplicitWaitTime()),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithName(
                name,
                text,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime());
        }
    }

    @Override
    public void populateElementWithName(final String name, final String text, final int waitTime) {
        ++interactionCount;

        MOUSE_MOVEMENT_UTILS.mouseGlide(
            getWebDriver(),
            (JavascriptExecutor) getWebDriver(),
            () -> getElementByName(name, waitTime),
            Constants.MOUSE_MOVE_TIME,
            Constants.MOUSE_MOVE_STEPS);

        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithName(
                name,
                text,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime);
        }
    }

    @Override
    public void clickElementIfExists(final String force, final String locator, final String ifExistsOption) {
        ++interactionCount;

        try {
            MOUSE_MOVEMENT_UTILS.mouseGlide(
                getWebDriver(),
                (JavascriptExecutor) getWebDriver(),
                () -> SIMPLE_BY.getElement(
                    getWebDriver(),
                    locator,
                    getDefaultExplicitWaitTime(),
                    by -> force == null
                        ? ExpectedConditions.elementToBeClickable(by)
                        : ExpectedConditions.presenceOfElementLocated(by)),
                Constants.MOUSE_MOVE_TIME,
                Constants.MOUSE_MOVE_STEPS,
                force != null);

            if (getAutomatedBrowser() != null) {
                getAutomatedBrowser().clickElementIfExists(
                    force,
                    locator,
                    SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                        Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime(),
                    ifExistsOption);
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void clickElementIfExists(final String force, final String locator, final Integer waitTime, final String ifExistsOption) {
        ++interactionCount;

        try {
            MOUSE_MOVEMENT_UTILS.mouseGlide(
                getWebDriver(),
                (JavascriptExecutor) getWebDriver(),
                () -> SIMPLE_BY.getElement(
                    getWebDriver(),
                    locator,
                    waitTime,
                    by -> force == null
                        ? ExpectedConditions.elementToBeClickable(by)
                        : ExpectedConditions.presenceOfElementLocated(by)),
                Constants.MOUSE_MOVE_TIME,
                Constants.MOUSE_MOVE_STEPS,
                force != null);

            if (getAutomatedBrowser() != null) {
                getAutomatedBrowser().clickElementIfExists(
                    force,
                    locator,
                    SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                        Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime,
                    ifExistsOption);
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void selectOptionByTextFromSelectIfExists(final String force, final String optionText, final String locator, final String ifExistsOption) {
        ++interactionCount;

        try {
            MOUSE_MOVEMENT_UTILS.mouseGlide(
                getWebDriver(),
                (JavascriptExecutor) getWebDriver(),
                () -> SIMPLE_BY.getElement(
                    getWebDriver(),
                    locator,
                    getDefaultExplicitWaitTime(),
                    ExpectedConditions::elementToBeClickable),
                Constants.MOUSE_MOVE_TIME,
                Constants.MOUSE_MOVE_STEPS);

            if (getAutomatedBrowser() != null) {
                getAutomatedBrowser().selectOptionByTextFromSelectIfExists(
                    force,
                    optionText,
                    locator,
                    SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                        Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime(),
                    ifExistsOption);
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void selectOptionByTextFromSelectIfExists(final String force, final String optionText, final String locator, final int waitTime, final String ifExistsOption) {
        ++interactionCount;

        try {
            MOUSE_MOVEMENT_UTILS.mouseGlide(
                getWebDriver(),
                (JavascriptExecutor) getWebDriver(),
                () -> SIMPLE_BY.getElement(
                    getWebDriver(),
                    locator,
                    waitTime,
                    ExpectedConditions::elementToBeClickable),
                Constants.MOUSE_MOVE_TIME,
                Constants.MOUSE_MOVE_STEPS);

            if (getAutomatedBrowser() != null) {
                getAutomatedBrowser().selectOptionByTextFromSelectIfExists(
                    force,
                    optionText,
                    locator,
                    SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                        Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime,
                    ifExistsOption);
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void selectOptionByValueFromSelectIfExists(final String force, final String optionValue, final String locator, final int waitTime, final String ifExistsOption) {
        ++interactionCount;

        try {
            MOUSE_MOVEMENT_UTILS.mouseGlide(
                getWebDriver(),
                (JavascriptExecutor) getWebDriver(),
                () -> SIMPLE_BY.getElement(
                    getWebDriver(),
                    locator,
                    waitTime,
                    ExpectedConditions::elementToBeClickable),
                Constants.MOUSE_MOVE_TIME,
                Constants.MOUSE_MOVE_STEPS);

            if (getAutomatedBrowser() != null) {
                getAutomatedBrowser().selectOptionByValueFromSelectIfExists(
                    force,
                    optionValue,
                    locator,
                    SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                        Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime,
                    ifExistsOption);
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void populateElement(final String force, final String locator, final String keystrokeDelay, final String text, final String ifExistsOption) {
        ++interactionCount;

        try {
            glideMouse(locator, getDefaultExplicitWaitTime(), ExpectedConditions::elementToBeClickable);

            if (getAutomatedBrowser() != null) {
                getAutomatedBrowser().populateElement(
                    force,
                    locator,
                    keystrokeDelay,
                    text,
                    SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                        Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime(),
                    ifExistsOption);
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void populateElement(final String force, final String locator, final String keystrokeDelay, final String text, final int waitTime, final String ifExistsOption) {
        ++interactionCount;

        try {
            glideMouse(locator, waitTime, ExpectedConditions::elementToBeClickable);

            if (getAutomatedBrowser() != null) {
                getAutomatedBrowser().populateElement(
                    force,
                    locator,
                    keystrokeDelay,
                    text,
                    SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                        Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime,
                    ifExistsOption);
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void mouseOverIfExists(final String force, final String locator, final String ifExistsOption) {
        ++interactionCount;

        try {
            glideMouse(locator, getDefaultExplicitWaitTime(), ExpectedConditions::presenceOfElementLocated);

            final Actions action = new Actions(getWebDriver());

            if (StringUtils.isNotBlank(force)) {
                final WebElement element = SIMPLE_BY.getElement(
                    getWebDriver(),
                    locator,
                    SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                        Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime(),
                    ExpectedConditions::presenceOfElementLocated);
                ((JavascriptExecutor) getWebDriver()).executeScript(
                    "arguments[0].dispatchEvent(new Event('mouseover', { bubbles: true }))",
                    element);
            } else {
                // Retry to address the org.openqa.selenium.StaleElementReferenceException exception
                RETRY_SERVICE.getTemplate().execute((RetryCallback<Void, WebElementException>) context -> {
                    final WebElement element = SIMPLE_BY.getElement(
                        getWebDriver(),
                        locator,
                        SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                            Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime(),
                        ExpectedConditions::presenceOfElementLocated);
                    action.moveToElement(element).perform();
                    return null;
                });
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void mouseOverIfExists(final String force, final String locator, final int waitTime, final String ifExistsOption) {
        ++interactionCount;

        try {
            glideMouse(locator, waitTime, ExpectedConditions::presenceOfElementLocated);

            final Actions action = new Actions(getWebDriver());
            final WebElement element = SIMPLE_BY.getElement(
                getWebDriver(),
                locator,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime,
                ExpectedConditions::presenceOfElementLocated);

            if (StringUtils.isNotBlank(force)) {
                ((JavascriptExecutor) getWebDriver()).executeScript(
                    "arguments[0].dispatchEvent(new Event('mouseover', { bubbles: true }))",
                    element);
            } else {
                action.moveToElement(element).perform();
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void focusIfExists(final String force, final String locator, final String ifExistsOption) {
        ++interactionCount;

        try {
            glideMouse(locator, getDefaultExplicitWaitTime(), ExpectedConditions::presenceOfElementLocated);

            final Actions action = new Actions(getWebDriver());
            final WebElement element = SIMPLE_BY.getElement(
                getWebDriver(),
                locator,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : getDefaultExplicitWaitTime(),
                ExpectedConditions::presenceOfElementLocated);
            if (StringUtils.isNotBlank(force)) {
                ((JavascriptExecutor) getWebDriver()).executeScript(
                    "arguments[0].dispatchEvent(new Event('focus', { bubbles: true }))",
                    element);
            } else {
                action.moveToElement(element).perform();
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void focusIfExists(final String force, final String locator, final int waitTime, final String ifExistsOption) {
        ++interactionCount;

        try {
            glideMouse(locator, waitTime, ExpectedConditions::presenceOfElementLocated);

            final Actions action = new Actions(getWebDriver());
            final WebElement element = SIMPLE_BY.getElement(
                getWebDriver(),
                locator,
                SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(
                    Constants.MOVE_CURSOR_TO_ELEMENT, false) ? 0 : waitTime,
                ExpectedConditions::presenceOfElementLocated);

            if (StringUtils.isNotBlank(force)) {
                ((JavascriptExecutor) getWebDriver()).executeScript(
                    "arguments[0].dispatchEvent(new Event('focus', { bubbles: true }))",
                    element);
            } else {
                action.moveToElement(element).perform();
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    /**
     * @return the number of user interactions we have simulated.
     */
    public int getInteractionCount() {
        return interactionCount;
    }
}
