FROM openjdk:8

WORKDIR /home/rundeck-mattermost-incoming-webhook-plugin
CMD ["bash", "./gradlew"]