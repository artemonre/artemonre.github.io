FROM openjdk:17-alpine

USER 1000

ENTRYPOINT ["/telegram_bot/bin/telegram_bot", "/telegram_bot/temp.config.json"]

ADD ./build/classes/artifacts/telegram_bot_jar/telegram_bot.jar /
ADD local.config.json /telegram_bot/