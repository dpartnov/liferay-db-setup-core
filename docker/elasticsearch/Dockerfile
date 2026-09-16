ARG ELASTICSEARCH_VERSION=8.19.11

FROM docker.elastic.co/elasticsearch/elasticsearch:${ELASTICSEARCH_VERSION}

RUN elasticsearch-plugin install --batch analysis-icu && \
    elasticsearch-plugin install --batch analysis-kuromoji && \
    elasticsearch-plugin install --batch analysis-smartcn && \
    elasticsearch-plugin install --batch analysis-stempel
