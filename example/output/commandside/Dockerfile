FROM jolielang/jolie:1.13.1
COPY CommandSide.ol .
COPY config.json .
EXPOSE 10000
EXPOSE 8080
CMD ["jolie", "--params", "config.json", "CommandSide.ol"]
