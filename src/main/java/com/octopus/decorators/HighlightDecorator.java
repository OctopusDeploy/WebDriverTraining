package com.octopus.decorators;

import com.octopus.AutomatedBrowser;
import com.octopus.Constants;
import com.octopus.decoratorbase.AutomatedBrowserBase;
import com.octopus.exceptions.WebElementException;
import com.octopus.utils.RetryService;
import com.octopus.utils.SimpleBy;
import com.octopus.utils.SystemPropertyUtils;
import com.octopus.utils.impl.RetryServiceImpl;
import com.octopus.utils.impl.SimpleByImpl;
import com.octopus.utils.impl.SystemPropertyUtilsImpl;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.retry.RetryCallback;

/**
 * A decorator to provide highlighting of HTML elements.
 */
public class HighlightDecorator extends AutomatedBrowserBase {
    private static final Logger LOGGER = Logger.getLogger(HighlightDecorator.class.toString());
    private static final SimpleBy SIMPLE_BY = new SimpleByImpl();
    private static final SystemPropertyUtils SYSTEM_PROPERTY_UTILS = new SystemPropertyUtilsImpl();
    private static final RetryService RETRY_SERVICE = new RetryServiceImpl();
    private int defaultOffset = 10;
    private Map<String, String> originalStyles = new HashMap<>();

    public HighlightDecorator() {
        super(null);
    }

    public HighlightDecorator(final AutomatedBrowser automatedBrowser) {
        super(automatedBrowser);
    }

    @Override
    public void setDefaultHighlightOffset(final String offset) {
        defaultOffset = NumberUtils.toInt(offset, defaultOffset);
    }

    @Override
    public void elementHighlightIfExists(final String location, final String lift, final String locator, final String offset, final String ifExistsOption) {
        this.elementHighlightIfExists(location, lift, locator, offset, getDefaultExplicitWaitTime(), ifExistsOption);
    }

    @Override
    public void elementHighlightIfExists(final String location, final String lift, final String locator, final String offset, final int waitTime, final String ifExistsOption) {
        try {
            if (SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(Constants.DISABLE_HIGHLIGHTS, false)) {
                if (waitTime != getDefaultExplicitWaitTime()) {
                    LOGGER.info("A highlight step has a custom wait time that is ignored because highlighting " +
                            "is disabled. If this step was used to wait for an element to be visible, the scenario " +
                            "may not work as expected.");
                }
                return;
            }

            final int offsetValue = NumberUtils.toInt(offset, defaultOffset);

            // This will catch StaleElementReferenceException exceptions and attempt to apply the highlight again
            RETRY_SERVICE.getTemplate().execute((RetryCallback<Void, WebElementException>)  context -> {
                final WebElement element = SIMPLE_BY.getElement(
                        getWebDriver(),
                        locator,
                        waitTime / 3,
                        ExpectedConditions::presenceOfElementLocated);

                originalStyles.put(locator, element.getAttribute("style"));

                if (StringUtils.equals(StringUtils.trim(location), "inside")) {
                    ((JavascriptExecutor) getWebDriver()).executeScript(
                            """
                                    arguments[0].style.border = '5px solid rgb(0, 204, 101)';
                                    """ + (StringUtils.isBlank(lift) ? "" : "arguments[0].style['z-index'] = 999999;"),
                            element);
                } else {
                    ((JavascriptExecutor) getWebDriver()).executeScript(
                            """
                                    arguments[0].style.outline = '5px solid rgb(0, 204, 101)';
                                    arguments[0].style['outline-offset'] = '""" + offsetValue + "px';" + """
                                    arguments[0].style['outline-style'] = 'solid';
                                    """ + (StringUtils.isBlank(lift) ? "" : "arguments[0].style['z-index'] = 999999;"),
                            element);
                }

                return null;
            });
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }

    @Override
    public void removeElementHighlight(final String locator, final String ifExistsOption) {
        removeElementHighlight(locator, getDefaultExplicitWaitTime(), ifExistsOption);
    }

    @Override
    public void removeElementHighlight(final String locator, final int waitTime, final String ifExistsOption) {
        if (SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(Constants.DISABLE_HIGHLIGHTS, false)) {
            return;
        }

        try {
            if (originalStyles.containsKey(locator)) {
                final WebElement element = SIMPLE_BY.getElement(
                        getWebDriver(),
                        locator,
                        waitTime,
                        by -> ExpectedConditions.presenceOfElementLocated(by));

                ((JavascriptExecutor) getWebDriver()).executeScript(
                        "arguments[0].setAttribute('style', '" + originalStyles.get(locator) + "');",
                        element);

                originalStyles.remove(locator);
            }
        } catch (final WebElementException | StaleElementReferenceException ex) {
            if (StringUtils.isEmpty(ifExistsOption)) {
                throw ex;
            }
        }
    }
}
