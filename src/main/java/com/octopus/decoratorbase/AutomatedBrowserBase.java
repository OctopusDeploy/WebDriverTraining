package com.octopus.decoratorbase;

import com.octopus.AutomatedBrowser;
import com.octopus.AutomatedBrowserFactory;
import com.octopus.Constants;
import com.octopus.exceptions.BrowserException;
import com.octopus.exceptions.SaveException;
import com.octopus.exceptions.ScriptException;
import com.octopus.utils.SystemPropertyUtils;
import com.octopus.utils.impl.SystemPropertyUtilsImpl;
import io.cucumber.core.api.Scenario;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class AutomatedBrowserBase implements AutomatedBrowser {
    private static final Logger LOGGER = Logger.getLogger(AutomatedBrowserBase.class.toString());
    static private final String LastReturn = "LastReturn";
    static private final AutomatedBrowserFactory AUTOMATED_BROWSER_FACTORY = new AutomatedBrowserFactory();
    private static final SystemPropertyUtils SYSTEM_PROPERTY_UTILS = new SystemPropertyUtilsImpl();
    private Map<String, String> aliases = new HashMap<>();
    static private Map<String, String> externalAliases = new HashMap<>();
    private AutomatedBrowser automatedBrowser;
    private static AutomatedBrowser sharedAutomatedBrowser;
    private static AutomatedBrowserBase instanceAutomatedBrowser;

    public static AutomatedBrowserBase getInstance() {
        return instanceAutomatedBrowser;
    }

    public AutomatedBrowserBase() {
        instanceAutomatedBrowser = this;
    }

    public AutomatedBrowserBase(final AutomatedBrowser automatedBrowser) {
        this.automatedBrowser = automatedBrowser;
    }

    public static void setExternalAliases(final Map<String, String> externalAliases) {
        if (externalAliases == null) return;
        AutomatedBrowserBase.externalAliases.putAll(externalAliases);
    }

    @Before
    public void reuseSharedBrowser() {
        automatedBrowser = sharedAutomatedBrowser;
    }

    @After
    public void afterScenario(final Scenario scenario) {
        if (scenario.isFailed()) {
            closeBrowser();
            stopScreenRecording();
            if (SYSTEM_PROPERTY_UTILS.getPropertyAsBoolean(Constants.DUMP_ALIASES_ON_FAILURE, false)) {
                dumpAliases();
            }
        }

        LOGGER.info("Recorded " + getInteractionCount() + " interactions for the scenario");
    }

    private Map<String, String> getAliases() {
        final Map<String, String> combinedAliases = new HashMap<>();
        combinedAliases.putAll(aliases);
        combinedAliases.putAll(externalAliases);
        return combinedAliases;
    }

    public AutomatedBrowser getAutomatedBrowser() {
        return automatedBrowser;
    }

    @And("^I run the feature \"([^\"]*)\"$")
    public void executeFeature(final String featureFile) {
        try {
            // java binary
            final String java = System.getProperty("java.home") + "/bin/java";
            // vm arguments
            final List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            final StringBuffer vmArgsOneLine = new StringBuffer();
            for (final String arg : vmArguments) {
                // if it's the agent argument : we ignore it otherwise the
                // address of the old application and the new one will be in conflict
                if (!arg.contains("-agentlib")) {
                    vmArgsOneLine.append(arg);
                    vmArgsOneLine.append(" ");
                }
            }
            // init the command to execute, add the vm args
            final StringBuffer cmd = new StringBuffer("\"" + java + "\" " + vmArgsOneLine);
            // program main and program arguments (be careful a sun property. might not be supported by all JVM)
            final String[] mainCommand = System.getProperty("sun.java.command").split(" ");
            // program main is a jar
            if (mainCommand[0].endsWith(".jar")) {
                // if it's a jar, add -jar mainJar
                cmd.append("-jar " + new File(mainCommand[0]).getPath());
            } else {
                // else it's a .class, add the classpath and mainClass
                cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
            }

            // First see if the supplied file is an absolute path, otherwise assume it is in the same directory as the current feature file
            cmd.append(" ");
            if (new File(featureFile).exists()) {
                cmd.append(featureFile);
            } else {
                final String featurePath = new File(mainCommand[mainCommand.length - 1]).getParentFile().getAbsolutePath();
                cmd.append(new File(featurePath, featureFile).getAbsolutePath());
            }

            final int result = Runtime.getRuntime().exec(cmd.toString()).waitFor();
            if (result != 0) {
                throw new ScriptException("Nested feature fiule did not return 0");
            }
        } catch (final IOException | InterruptedException ex) {
            throw new ScriptException("Failed to run nested feature file.", ex);
        }
    }

    @Given("^I set the following aliases:$")
    public void setAliases(Map<String, String> aliases) {
        this.aliases.putAll(aliases);
    }

    @Override
    @And("^I (?:sleep|wait) for \"([^\"]*)\" seconds?$")
    public void sleep(String seconds) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().sleep(getAliases().getOrDefault(seconds, seconds));
        }
    }

    @Given("^I open the( shared)? browser \"([^\"]*)\"$")
    public void openBrowser(String shared, String browser) {
        if (sharedAutomatedBrowser != null) {
            throw new BrowserException("Can not open a browser with an existing shared browser.");
        }

        if (shared != null) {
            automatedBrowser = sharedAutomatedBrowser = AUTOMATED_BROWSER_FACTORY.getAutomatedBrowser(browser);
            automatedBrowser.init();
        } else {
            automatedBrowser = AUTOMATED_BROWSER_FACTORY.getAutomatedBrowser(browser);
            automatedBrowser.init();
        }
    }

    @Given("^I close the browser$")
    public void closeBrowser() {
        if (automatedBrowser != null) {
            automatedBrowser.destroy();
        }

        automatedBrowser = null;
        sharedAutomatedBrowser = null;
    }

    @And("^I set the default explicit wait time to \"(\\d+)\" seconds?$")
    @Override
    public void setDefaultExplicitWaitTime(int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().setDefaultExplicitWaitTime(waitTime);
        }
    }

    @Override
    public int getDefaultExplicitWaitTime() {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getDefaultExplicitWaitTime();
        }

        return 0;
    }

    @Override
    public WebDriver getWebDriver() {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getWebDriver();
        }

        return null;
    }

    @Override
    public void setWebDriver(final WebDriver webDriver) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().setWebDriver(webDriver);
        }
    }

    @Override
    public DesiredCapabilities getDesiredCapabilities() {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getDesiredCapabilities();
        }

        return new DesiredCapabilities();
    }

    @Override
    public void init() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().init();
        }
    }

    @Override
    public void destroy() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().destroy();
        }
    }

    @And("^I open the URL \"([^\"]*)\"$")
    @Override
    public void goTo(String url) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().goTo(getAliases().getOrDefault(url, url));
        }
    }

    @And("^I refresh the page$")
    @Override
    public void refresh() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().refresh();
        }
    }

    @And("^I start recording the screen to the directory \"([^\"]*)\"$")
    @Override
    public void startScreenRecording(final String file) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().startScreenRecording(getAliases().getOrDefault(file, file));
        }
    }

    @And("^I stop recording the screen$")
    @Override
    public void stopScreenRecording() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().stopScreenRecording();
        }
    }

    @And("^I dump the aliases$")
    @Override
    public void dumpAliases() {
        LOGGER.info("\nStart of alias dump");
        getAliases().entrySet().forEach(entrySet -> LOGGER.info(entrySet.getKey() + ": " + entrySet.getValue()));
    }

    @And("^I write the value of the alias \"([^\"]*)\" to the file \"([^\"]*)\"$")
    @Override
    public void writeAliasValueToFile(final String alias, final String filename) {
        try {
            FileUtils.write(
                    new File(filename),
                    getAliases().getOrDefault(alias, ""),
                    StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new SaveException("Failed to write alias value to " + filename, ex);
        }
    }

    @Override
    public CompletableFuture<Void> takeScreenshot(final String filename, boolean force) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().takeScreenshot(
                    getAliases().getOrDefault(filename, filename),
                    force);
        }
        return CompletableFuture.completedFuture(null);
    }

    @And("^I save a screenshot to \"([^\"]*)\"$")
    @Override
    public CompletableFuture<Void> takeScreenshot(final String filename) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().takeScreenshot(getAliases().getOrDefault(filename, filename));
        }
        return CompletableFuture.completedFuture(null);
    }

    @And("^I save a screenshot to \"([^\"]*)\" called \"([^\"]*)\"$")
    @Override
    public CompletableFuture<Void> takeScreenshot(final String directory, final String filename) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().takeScreenshot(
                    getAliases().getOrDefault(directory, directory),
                    getAliases().getOrDefault(filename, filename));
        }
        return CompletableFuture.completedFuture(null);
    }

    @And("^I set the window size to \"([^\"]*)\" x \"([^\"]*)\"$")
    @Override
    public void setWindowSize(final String width, final String height) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().setWindowSize(
                    getAliases().getOrDefault(width, width),
                    getAliases().getOrDefault(height, height));
        }
    }

    @And("^I click the \\w+(?:\\s+\\w+)* with the id \"([^\"]*)\"$")
    @Override
    public void clickElementWithId(final String id) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithId(getAliases().getOrDefault(id, id));
        }
    }

    @And("^I click the \\w+(?:\\s+\\w+)* with the id \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void clickElementWithId(final String id, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithId(getAliases().getOrDefault(id, id), waitTime);
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \\w+(?:\\s+\\w+)* with the id \"([^\"]*)\"$")
    @Override
    public void selectOptionByTextFromSelectWithId(final String optionText, final String id) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithId(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(id, id));
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \\w+(?:\\s+\\w+)* with the id \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void selectOptionByTextFromSelectWithId(final String optionText, final String id, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithId(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(id, id),
                    waitTime);
        }
    }

    @And("^I populate the \\w+(?:\\s+\\w+)* with the id \"([^\"]*)\" with(?: the text)? \"([^\"]*)\"$")
    @Override
    public void populateElementWithId(final String id, final String text) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithId(
                    getAliases().getOrDefault(id, id),
                    getAliases().getOrDefault(text, text));
        }
    }

    @And("^I populate the \\w+(?:\\s+\\w+)* with the id \"([^\"]*)\" with(?: the text)? \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void populateElementWithId(final String id, final String text, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithId(
                    getAliases().getOrDefault(id, id),
                    getAliases().getOrDefault(text, text),
                    waitTime);
        }
    }

    @Override
    public String getTextFromElementWithId(final String id) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getTextFromElementWithId(getAliases().getOrDefault(id, id));
        }

        return null;
    }

    @Override
    public String getTextFromElementWithId(final String id, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getTextFromElementWithId(getAliases().getOrDefault(id, id), waitTime);
        }

        return null;
    }

    @And("^I click the \\w+(?:\\s+\\w+)* with the xpath \"([^\"]*)\"$")
    @Override
    public void clickElementWithXPath(final String xpath) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithXPath(getAliases().getOrDefault(xpath, xpath));
        }
    }

    @And("^I click the \\w+(?:\\s+\\w+)* with the xpath \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void clickElementWithXPath(final String xpath, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithXPath(getAliases().getOrDefault(xpath, xpath), waitTime);
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \\w+(?:\\s+\\w+)* with the xpath \"([^\"]*)\"$")
    @Override
    public void selectOptionByTextFromSelectWithXPath(final String optionText, final String xpath) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithXPath(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(xpath, xpath));
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \\w+(?:\\s+\\w+)* with the xpath \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void selectOptionByTextFromSelectWithXPath(final String optionText, final String xpath, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithXPath(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(xpath, xpath),
                    waitTime);
        }
    }

    @And("^I populate the \\w+(?:\\s+\\w+)* with the xpath \"([^\"]*)\" with(?: the text)? \"([^\"]*)\"$")
    @Override
    public void populateElementWithXPath(final String xpath, final String text) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithXPath(
                    getAliases().getOrDefault(xpath, xpath),
                    getAliases().getOrDefault(text, text));
        }
    }

    @And("^I populate the \\w+(?:\\s+\\w+)* with the xpath \"([^\"]*)\" with(?: the text)? \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void populateElementWithXPath(final String xpath, final String text, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithXPath(
                    getAliases().getOrDefault(xpath, xpath),
                    getAliases().getOrDefault(text, text),
                    waitTime);
        }
    }

    @Override
    public String getTextFromElementWithXPath(final String xpath) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getTextFromElementWithXPath(getAliases().getOrDefault(xpath, xpath));
        }

        return null;
    }

    @Override
    public String getTextFromElementWithXPath(final String xpath, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getTextFromElementWithXPath(getAliases().getOrDefault(xpath, xpath), waitTime);
        }

        return null;
    }

    @And("^I click the \\w+(?:\\s+\\w+)* with the css selector \"([^\"]*)\"$")
    @Override
    public void clickElementWithCSSSelector(final String cssSelector) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithCSSSelector(getAliases().getOrDefault(cssSelector, cssSelector));
        }
    }

    @And("^I click the \\w+(?:\\s+\\w+)* with the css selector \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void clickElementWithCSSSelector(final String cssSelector, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithCSSSelector(getAliases().getOrDefault(cssSelector, cssSelector), waitTime);
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \\w+(?:\\s+\\w+)* with the css selector \"([^\"]*)\"$")
    @Override
    public void selectOptionByTextFromSelectWithCSSSelector(final String optionText, final String cssSelector) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithCSSSelector(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(cssSelector, cssSelector));
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \\w+(?:\\s+\\w+)* with the css selector \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void selectOptionByTextFromSelectWithCSSSelector(final String optionText, final String cssSelector, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithCSSSelector(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(cssSelector, cssSelector),
                    waitTime);
        }
    }

    @And("^I populate the \\w+(?:\\s+\\w+)* with the css selector \"([^\"]*)\" with(?: the text)? \"([^\"]*)\"$")
    @Override
    public void populateElementWithCSSSelector(final String cssSelector, final String text) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithCSSSelector(
                    getAliases().getOrDefault(cssSelector, cssSelector),
                    getAliases().getOrDefault(text, text));
        }
    }

    @And("^I populate the \\w+(?:\\s+\\w+)* with the css selector \"([^\"]*)\" with(?: the text)? \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void populateElementWithCSSSelector(final String cssSelector, final String text, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithCSSSelector(
                    getAliases().getOrDefault(cssSelector, cssSelector),
                    getAliases().getOrDefault(text, text),
                    waitTime);
        }
    }

    @Override
    public String getTextFromElementWithCSSSelector(final String cssSelector) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getTextFromElementWithCSSSelector(
                    getAliases().getOrDefault(cssSelector, cssSelector));
        }

        return null;
    }

    @Override
    public String getTextFromElementWithCSSSelector(final String cssSelector, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getTextFromElementWithCSSSelector(
                    getAliases().getOrDefault(cssSelector, cssSelector),
                    waitTime);
        }

        return null;
    }

    @And("^I click the \\w+(?:\\s+\\w+)* with the name \"([^\"]*)\"$")
    @Override
    public void clickElementWithName(final String name) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithName(getAliases().getOrDefault(name, name));
        }
    }

    @And("^I click the \\w+(?:\\s+\\w+)* with the name \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void clickElementWithName(final String name, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementWithName(getAliases().getOrDefault(name, name), waitTime);
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \\w+(?:\\s+\\w+)* with the name \"([^\"]*)\"$")
    @Override
    public void selectOptionByTextFromSelectWithName(final String optionText, final String name) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithName(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(name, name));
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \\w+(?:\\s+\\w+)* with the name \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void selectOptionByTextFromSelectWithName(final String optionText, final String name, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectWithName(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(name, name),
                    waitTime);
        }
    }

    @And("^I populate the \\w+(?:\\s+\\w+)* with the name \"([^\"]*)\" with(?: the text)? \"([^\"]*)\"$")
    @Override
    public void populateElementWithName(final String name, final String text) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithName(
                    getAliases().getOrDefault(name, name),
                    getAliases().getOrDefault(text, text));
        }
    }

    @And("^I populate the \\w+(?:\\s+\\w+)* with the name \"([^\"]*)\" with(?: the text)? \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?$")
    @Override
    public void populateElementWithName(final String name, final String text, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElementWithName(
                    getAliases().getOrDefault(name, name),
                    getAliases().getOrDefault(text, text),
                    waitTime);
        }
    }

    @Override
    public String getTextFromElementWithName(final String name) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getTextFromElementWithName(getAliases().getOrDefault(name, name));
        }

        return null;
    }

    @Override
    public String getTextFromElementWithName(final String name, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getTextFromElementWithName(
                    getAliases().getOrDefault(name, name),
                    waitTime);
        }

        return null;
    }

    @And("^I( force)? click the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?( if it exists)?$")
    @Override
    public void clickElementIfExists(final String force, final String locator, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementIfExists(
                    force,
                    getAliases().getOrDefault(locator, locator),
                    ifExistsOption);
        }
    }

    @And("^I( force)? click the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public void clickElementIfExists(final String force, final String locator, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clickElementIfExists(
                    force,
                    getAliases().getOrDefault(locator, locator),
                    waitTime,
                    ifExistsOption);
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?( if it exists)?$")
    @Override
    public void selectOptionByTextFromSelectIfExists(final String optionText, final String locator, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectIfExists(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(locator, locator),
                    ifExistsOption);
        }
    }

    @And("^I select the option \"([^\"]*)\" from the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public void selectOptionByTextFromSelectIfExists(final String optionText, final String locator, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByTextFromSelectIfExists(
                    getAliases().getOrDefault(optionText, optionText),
                    getAliases().getOrDefault(locator, locator),
                    waitTime,
                    ifExistsOption);
        }
    }

    @And("^I select the option value \"([^\"]*)\" from the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?( if it exists)?$")
    @Override
    public void selectOptionByValueFromSelectIfExists(final String optionValue, final String locator, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByValueFromSelectIfExists(
                    getAliases().getOrDefault(optionValue, optionValue),
                    getAliases().getOrDefault(locator, locator),
                    ifExistsOption);
        }
    }

    @And("^I select the option value \"([^\"]*)\" from the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public void selectOptionByValueFromSelectIfExists(final String optionValue, final String locator, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().selectOptionByValueFromSelectIfExists(
                    getAliases().getOrDefault(optionValue, optionValue),
                    getAliases().getOrDefault(locator, locator),
                    waitTime,
                    ifExistsOption);
        }
    }

    // A hack to match multiple regexes
    // https://github.com/cucumber/cucumber-jvm/issues/1341#issuecomment-379521254
    @And("^I populate the \"([^\"]*)\" \\w+(?:\\s+\\w+)* with(?: the text)? \"([^\"]*)\"( if it exists)?$")
    @Then("^I populate the \"([^\"]*)\" \\w+(?:\\s+\\w+)* with(?: the text)?:( if it exists)?$")
    @Override
    public void populateElement(final String locator, final String text, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElement(
                    getAliases().getOrDefault(locator, locator),
                    getAliases().getOrDefault(text, text),
                    ifExistsOption);
        }
    }

    @And("^I populate the \"([^\"]*)\" \\w+(?:\\s+\\w+)* with(?: the text)? \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public void populateElement(final String locator, final String text, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().populateElement(
                    getAliases().getOrDefault(locator, locator),
                    getAliases().getOrDefault(text, text),
                    waitTime,
                    ifExistsOption);
        }
    }

    @And("^I clear the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?( if it exists)?$")
    @Override
    public void clearIfExists(final String locator, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clearIfExists(
                    getAliases().getOrDefault(locator, locator),
                    ifExistsOption);
        }
    }

    @And("^I clear the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public void clearIfExists(final String locator, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clearIfExists(
                    getAliases().getOrDefault(locator, locator),
                    waitTime,
                    ifExistsOption);
        }
    }

    @And("^I scroll down \"([^\"]*)\" px$")
    @Override
    public void scrollDown(final String distance) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().scrollDown(
                    getAliases().getOrDefault(distance, distance));
        }
    }

    @And("^I scroll up \"([^\"]*)\" px$")
    @Override
    public void scrollUp(final String distance) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().scrollUp(
                    getAliases().getOrDefault(distance, distance));
        }
    }

    @Then("^I verify the (current )?URL matches the regex \"([^\"]*)\"$")
    @Override
    public void verifyUrl(final String regex) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().verifyUrl(getAliases().getOrDefault(regex, regex));
        }
    }

    @And("^I zoom the browser in$")
    @Override
    public void browserZoomIn() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().browserZoomIn();
        }
    }

    @And("^I zoom the browser out$")
    @Override
    public void browserZoomOut() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().browserZoomOut();
        }
    }

    @And("^I mouse over the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?( if it exists)?")
    @Override
    public void mouseOverIfExists(final String locator, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().mouseOverIfExists(getAliases().getOrDefault(locator, locator), ifExistsOption);
        }
    }

    @And("^I mouse over the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds?( if it exists)?")
    @Override
    public void mouseOverIfExists(final String locator, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().mouseOverIfExists(
                    getAliases().getOrDefault(locator, locator),
                    waitTime,
                    ifExistsOption);
        }
    }

    @And("^I get the text from the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?( if it exists)?$")
    @Override
    public String getTextFromElementIfExists(final String locator, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            final String text = getAutomatedBrowser().getTextFromElementIfExists(
                    getAliases().getOrDefault(locator, locator),
                    ifExistsOption);
            aliases.put(LastReturn, text);
            return text;
        }

        aliases.put(LastReturn, null);
        return null;
    }

    @And("^I get the text from the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public String getTextFromElementIfExists(final String locator, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            final String text = getAutomatedBrowser().getTextFromElementIfExists(
                    getAliases().getOrDefault(locator, locator),
                    waitTime,
                    ifExistsOption);
            aliases.put(LastReturn, text);
            return text;
        }

        aliases.put(LastReturn, null);
        return null;
    }

    @And("^I get group \"([^\"]*)\" from the regex \"([^\"]*)\" applied to text from the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?( if it exists)?$")
    @Override
    public String getRegexGroupFromElementIfExists(
            final String group,
            final String regex,
            final String locator,
            final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            final String text = getAutomatedBrowser().getRegexGroupFromElementIfExists(
                    getAliases().getOrDefault(group, group),
                    getAliases().getOrDefault(regex, regex),
                    getAliases().getOrDefault(locator, locator),
                    ifExistsOption);
            aliases.put(LastReturn, text);
            return text;
        }

        aliases.put(LastReturn, null);
        return null;
    }

    @And("^I get group \"([^\"]*)\" from the regex \"([^\"]*)\" applied to text from the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public String getRegexGroupFromElementIfExists(
            final String group,
            final String regex,
            final String locator,
            final int waitTime,
            final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            final String text = getAutomatedBrowser().getRegexGroupFromElementIfExists(
                    getAliases().getOrDefault(group, group),
                    getAliases().getOrDefault(regex, regex),
                    getAliases().getOrDefault(locator, locator),
                    waitTime,
                    ifExistsOption);
            aliases.put(LastReturn, text);
            return text;
        }

        aliases.put(LastReturn, null);
        return null;
    }

    @Then("^I verify the text from the \"([^\"]*)\" \\w+(?:\\s+\\w+)* matches the regex \"([^\"]*)\"( if it exists)?$")
    @Override
    public void verifyTextFromElementIfExists(final String locator, final String regex, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().verifyTextFromElementIfExists(
                    getAliases().getOrDefault(locator, locator),
                    getAliases().getOrDefault(regex, regex),
                    ifExistsOption);
        }
    }

    @Then("^I verify the text from the \"([^\"]*)\" \\w+(?:\\s+\\w+)* matches the regex \"([^\"]*)\" waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public void verifyTextFromElementIfExists(final String locator, final String regex, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().verifyTextFromElementIfExists(
                    getAliases().getOrDefault(locator, locator),
                    getAliases().getOrDefault(regex, regex),
                    waitTime,
                    ifExistsOption);
        }
    }

    @Override
    @And("^I scroll the \"([^\"]*)\" \\w+(?:\\s+\\w+)* into view(?: offset by \"([^\"]*)\")?( if it exists)?$")
    public void scrollElementIntoViewIfExists(final String locator, final String offset, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().scrollElementIntoViewIfExists(
                    getAliases().getOrDefault(locator, locator),
                    getAliases().getOrDefault(offset, offset),
                    ifExistsOption);
        }
    }

    @Override
    @And("^I scroll the \"([^\"]*)\" \\w+(?:\\s+\\w+)* into view(?: offset by \"([^\"]*)\")? waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    public void scrollElementIntoViewIfExists(final String locator, final String offset, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().scrollElementIntoViewIfExists(
                    getAliases().getOrDefault(locator, locator),
                    getAliases().getOrDefault(offset, offset),
                    waitTime,
                    ifExistsOption);
        }
    }

    @And("^I capture the HAR file$")
    @Override
    public void captureHarFile() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().captureHarFile();
        }
    }

    @And("^I capture the complete HAR file$")
    @Override
    public void captureCompleteHarFile() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().captureCompleteHarFile();
        }
    }

    @And("^I save the HAR file to \"([^\"]*)\"$")
    @Override
    public void saveHarFile(final String file) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().saveHarFile(getAliases().getOrDefault(file, file));
        }
    }

    @And("^I block the request to \"([^\"]*)\" returning the HTTP code \"\\d+\"$")
    @Override
    public void blockRequestTo(final String url, final int responseCode) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().blockRequestTo(
                    getAliases().getOrDefault(url, url),
                    responseCode);
        }
    }

    @And("^I alter the response from \"([^\"]*)\" returning the HTTP code \"\\d+\" and the response body:$")
    @Override
    public void alterResponseFrom(final String url, final int responseCode, final String responseBody) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().alterResponseFrom(
                    getAliases().getOrDefault(url, url),
                    responseCode,
                    getAliases().getOrDefault(responseBody, responseBody));
        }
    }

    @And("^I maximize the window$")
    @Override
    public void maximizeWindow() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().maximizeWindow();
        }
    }

    @And("^I highlight( outside| inside)? the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?(?: with an offset of \"([^\"]*)\")?( if it exists)?$")
    @Override
    public void elementHighlightIfExists(final String position, final String locator, final String offset, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().elementHighlightIfExists(
                    position,
                    getAliases().getOrDefault(locator, locator),
                    getAliases().getOrDefault(offset, offset),
                    ifExistsOption);
        }
    }

    @And("^I highlight( outside| inside)? the \"([^\"]*)\" \\w+(?:\\s+\\w+)*?(?: with an offset of \"([^\"]*)\")? waiting up to \"(\\d+)\" seconds?( if it exists)?$")
    @Override
    public void elementHighlightIfExists(final String position, final String locator, final String offset, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().elementHighlightIfExists(
                    position,
                    getAliases().getOrDefault(locator, locator),
                    getAliases().getOrDefault(offset, offset),
                    waitTime,
                    ifExistsOption);
        }
    }

    @And("^I remove the highlight from the \"([^\"]*)\" \\w+(?:\\s+\\w+)*$")
    @Override
    public void removeElementHighlight(final String locator) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().removeElementHighlight(getAliases().getOrDefault(locator, locator));
        }
    }

    @And("^I remove the highlight from the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds$")
    @Override
    public void removeElementHighlight(final String locator, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().removeElementHighlight(getAliases().getOrDefault(locator, locator), waitTime);
        }
    }

    @Then("^I verify the \"([^\"]*)\" \\w+(?:\\s+\\w+)* is present( if it exists)?$")
    @Override
    public void verifyElementExists(final String locator, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().verifyElementExists(
                    getAliases().getOrDefault(locator, locator),
                    ifExistsOption);
        }
    }

    @Then("^I verify the \"([^\"]*)\" \\w+(?:\\s+\\w+)* is present waiting up to \"(\\d+)\" seconds( if it exists)?$")
    @Override
    public void verifyElementExists(final String locator, final int waitTime, final String ifExistsOption) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().verifyElementExists(
                    getAliases().getOrDefault(locator, locator),
                    waitTime,
                    ifExistsOption);
        }
    }

    @Then("^I verify the \"([^\"]*)\" \\w+(?:\\s+\\w+)* is not present$")
    @Override
    public void verifyElementDoesNotExist(final String locator) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().verifyElementDoesNotExist(
                    getAliases().getOrDefault(locator, locator));
        }
    }

    @Then("^I verify the \"([^\"]*)\" \\w+(?:\\s+\\w+)* is not present waiting up to \"(\\d+)\" seconds$")
    @Override
    public void verifyElementDoesNotExist(final String locator, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().verifyElementDoesNotExist(
                    getAliases().getOrDefault(locator, locator),
                    waitTime);
        }
    }

    @And("^I press the escape key (?:on|in|from) the \"([^\"]*)\" \\w+(?:\\s+\\w+)*$")
    @Override
    public void pressEscape(final String locator) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().pressEscape(getAliases().getOrDefault(locator, locator));
        }
    }

    @And("^I press the escape key (?:on|in|from) the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds$")
    @Override
    public void pressEscape(final String locator, final int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().pressEscape(getAliases().getOrDefault(locator, locator), waitTime);
        }
    }

    @And("^I press the enter key (?:on|in|from) the \"([^\"]*)\" \\w+(?:\\s+\\w+)*$")
    @Override
    public void pressEnter(final String locator) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().pressEnter(getAliases().getOrDefault(locator, locator));
        }
    }

    @And("^I press the enter key (?:on|in|from) the \"([^\"]*)\" \\w+(?:\\s+\\w+)* waiting up to \"(\\d+)\" seconds$")
    @Override
    public void pressEnter(final String locator, int waitTime) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().pressEnter(getAliases().getOrDefault(locator, locator), waitTime);
        }
    }

    @Then("^I clear the transition$")
    @Override
    public void clearTransition() {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().clearTransition();
        }
    }

    @Then("^I fade the screen to \"([^\"]*)\" \"([^\"]*)\" \"([^\"]*)\" over \"([^\"]*)\" milliseconds$")
    @Override
    public void fadeScreen(String red, String green, String blue, String duration) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().fadeScreen(
                    getAliases().getOrDefault(red, red),
                    getAliases().getOrDefault(green, green),
                    getAliases().getOrDefault(blue, blue),
                    getAliases().getOrDefault(duration, duration));
        }
    }

    @Then("^I display a note with the text \"([^\"]*)\" for \"([^\"]*)\" seconds?")
    @Override
    public void displayNote(final String text, final String duration) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().displayNote(
                    getAliases().getOrDefault(text, text),
                    getAliases().getOrDefault(duration, duration));
        }
    }

    @And("^I run the following JavaScript:$")
    @Override
    public void runJavascript(final String code) {
        if (getAutomatedBrowser() != null) {
            getAutomatedBrowser().runJavascript(
                    getAliases().getOrDefault(code, code));
        }
    }

    public int getInteractionCount() {
        if (getAutomatedBrowser() != null) {
            return getAutomatedBrowser().getInteractionCount();
        }
        return -1;
    }
}