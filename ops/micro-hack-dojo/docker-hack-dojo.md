# I just want to execute the random 0815 Docker container

 
docker rm -f hidden-insight && docker run -v /root:/root --name hidden-insight evil-docker-image 

# What actually happened

sudo ls -lash  /root/could-be-your-SSH-key.txt

docker build -t evil-docker-image .


# Now, everything you have learned will be relativized!
    ...and why all the security buzz should be handled with common sense.   

docker rm -f hidden-insight && docker run --user 1000 -v /root:/root --name hidden-insight evil-docker-image