package ru.netology.test;

import com.codeborne.selenide.Configuration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.netology.data.DataHelper;
import ru.netology.data.SQLHelper;
import ru.netology.page.LoginPage;

import static com.codeborne.selenide.Selenide.open;
import static org.testng.AssertJUnit.assertEquals;

public class AuthTest {
    DataHelper.UserData user;
    DataHelper.VerifyCode code;
    LoginPage loginPage;

    @BeforeMethod
    public void setUp() {
       // Configuration.holdBrowserOpen = true;
        open("http://localhost:9999/");
        user = DataHelper.getUser();
        loginPage = new LoginPage();
    }

    @AfterMethod
    public void setDown() {
        SQLHelper.reloadVerifyCodeTable();
        SQLHelper.setUserStatus(user.getLogin(), "active");
    }

    @AfterClass
    public void setDownClass() {
        SQLHelper.setDown();
    }

    @Test(description = "Успешная авторизация")
    public void shouldAuth() {
        loginPage.insert(user.getLogin(), user.getPassword());
        var verifyPage = loginPage.success();
        code = DataHelper.getValidCode(user.getLogin());
        verifyPage.insert(code.getVerifyCode());
        var dashboardPage = verifyPage.success();
    }

    @Test
    public void shouldNoAuthWithOldestVerifyCode() {
        loginPage.insert(user.getLogin(), user.getPassword());
        var verifyPage = loginPage.success();
        code = DataHelper.getValidCode(user.getLogin());
        verifyPage.insert(code.getVerifyCode());
        var dashboardPage = verifyPage.success();

        open("http://localhost:9999/");
        loginPage.insert(user.getLogin(), user.getPassword());
        verifyPage = loginPage.success();
        verifyPage.insert(code.getVerifyCode());
        verifyPage.failed();
    }

    @Test
    public void shouldNoAuthWithInvalidVerifyCode() {
        loginPage.insert(user.getLogin(), user.getPassword());
        var verifyPage = loginPage.success();
        code = DataHelper.getRandomCode();
        verifyPage.insert(code.getVerifyCode());
        verifyPage.failed();
    }

    @Test
    public void shouldNoAuthWithInvalidPassword() {
        var password = DataHelper.getRandomPassword();
        loginPage.insert(user.getLogin(), password);
        loginPage.failed();
    }

    @Test
    public void shouldNoAuthWithInvalidLogin() {
        var login = DataHelper.getRandomLogin();
        loginPage.insert(login, user.getPassword());
        loginPage.failed();
    }

    @Test
    public void shouldBlockUserAfterThreeInputInvalidPassword() {
        var password = DataHelper.getRandomPassword();
        loginPage.insert(user.getLogin(), password);
        loginPage.failed();

        open("http://localhost:9999/");
        password = DataHelper.getRandomPassword();
        loginPage.insert(user.getLogin(), password);
        loginPage.failed();

        open("http://localhost:9999/");
        password = DataHelper.getRandomPassword();
        loginPage.insert(user.getLogin(), password);
        loginPage.failed();

        assertEquals(SQLHelper.getUserStatus(user.getLogin()), "blocked");

        open("http://localhost:9999/");
        loginPage.insert(user.getLogin(), user.getPassword());
        loginPage.blocked();
    }

    @Test
    public void shouldNotificationWithEmptyVerifyCode() {
        loginPage.insert(user.getLogin(), user.getPassword());
        var verifyPage = loginPage.success();
        verifyPage.insert(null);
        verifyPage.emptyCode();
    }

    @Test
    public void shouldNotificationWithEmptyPassword() {
        loginPage.insert(user.getLogin(), null);
        loginPage.emptyPassword();
    }

    @Test
    public void shouldNotificationWithEmptyLogin() {
        loginPage.insert(null, user.getPassword());
        loginPage.emptyLogin();
    }
}
