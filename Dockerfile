FROM openjdk:17-alpine

USER 1000

ENTRYPOINT ["/telegram_bot/bin/telegram_bot", "/telegram_bot/temp.config.json"]

ADD ./build/distributions/telegram_bot.tar /
ADD ./temp.config.json /telegram_bot/