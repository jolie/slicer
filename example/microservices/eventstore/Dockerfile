FROM jolielang/jolie:1.12.1
COPY EventStore.ol .
COPY config.json .
EXPOSE 10000
CMD ["jolie", "--params", "config.json", "EventStore.ol"]
