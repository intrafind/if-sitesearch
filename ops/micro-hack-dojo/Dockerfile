FROM debian:buster

RUN apt-get update && apt-get install curl -y

#RUN groupadd -g 1000 user && \
#    useradd -r -u 1000 -g user user
#USER user

#VOLUME /root

EXPOSE 4242

COPY remote-copy.sh .

ENTRYPOINT [ "sh", "./remote-copy.sh" ]