FROM jolielang/jolie:1.11.2-dev AS build
USER root
WORKDIR /app

RUN npm install -g @jolie/slicer

ENTRYPOINT ["jolieslicer"]
