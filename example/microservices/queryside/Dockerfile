FROM jolielang/jolie:1.12.1
COPY QuerySide.ol .
COPY config.json .
EXPOSE 10000
CMD ["jolie", "--params", "config.json", "QuerySide.ol"]
