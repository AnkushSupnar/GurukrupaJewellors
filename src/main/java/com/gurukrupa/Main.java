package com.gurukrupa;

import com.gurukrupa.data.entities.ShopInfo;
import com.gurukrupa.data.service.LoginUserService;
import com.gurukrupa.data.service.ShopService;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class Main extends Application {
	private ConfigurableApplicationContext springContext;
	protected StageManager stageManager;

	ShopService shopService;
	public static void main(String[] args) {

		Application.launch(args);
		//SpringApplication.run(Main.class, args);
	}

	@Override
	public void init() throws IOException {
		springContext = bootstrapSpringApplicationContext();

	}
	@Override
	public void start(Stage stage) throws Exception {
		stageManager  = springContext.getBean(StageManager.class,stage);
		//loginService = springContext.getBean(LoginService.class);
		shopService = springContext.getBean(ShopService.class);
		displayInitialScene();
	}
	@Override
	public void stop()
	{
		springContext.close();
	}
	protected void displayInitialScene() {

		/*ShopInfo shopInfo = shopService.getShopInfo();
		if (shopInfo == null) {
			stageManager.switchScene(FxmlView.CREATE_SHOPE);
		} else {
			stageManager.switchScene(FxmlView.LOGIN);
		}*/

		stageManager.switchScene(FxmlView.DASHBOARD);




	}
	private ConfigurableApplicationContext bootstrapSpringApplicationContext() {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(Main.class);
		String[] args = getParameters().getRaw().stream().toArray(String[]::new);
		builder.headless(false); //needed for TestFX integration testing or eles will get a java.awt.HeadlessException during tests
		return builder.run(args);
	}
}