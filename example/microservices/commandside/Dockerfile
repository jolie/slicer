FROM jolielang/jolie:1.12.1
COPY CommandSide.ol .
COPY config.json .
EXPOSE 10000
EXPOSE 10001
CMD ["jolie", "--params", "config.json", "CommandSide.ol"]
