build-locally:
	rm -rf ./build
	chmod +x gradlew && bash ./gradlew

build-docker:
	rm -rf ./build && mkdir -p ./build
	docker build -t rundeck-slack-incoming-webhook-plugin:latest .
	docker container create --name build_slack_webhook_plugin rundeck-slack-incoming-webhook-plugin:latest
	docker container cp build_slack_webhook_plugin:/app/build/libs ${PWD}/build/libs
	docker container rm build_slack_webhook_plugin
	docker image rm rundeck-slack-incoming-webhook-plugin:latest
