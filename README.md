# TorUsage

This project aims to visualize current and past usage of the Tor network. Our first goal is to display the amount of public Tor nodes per country.

## Development

### Dependencies

On`Linux` systems you can use the install script `./install`.
Supported OS are `Debian/Ubuntu`, `RHEL/CentOS/Fedora`, `OpenSUSE`, `ArchLinux`.

Otherwise install these manually:

- [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
  / [OpenJDK](https://openjdk.java.net/install/index.html) >= Java version 11
- [yarn](https://yarnpkg.com/en/docs/install)

### Run

- Linux: type `./run`
- Windows: `run.bat`

## Database setup
### Uninstall old versions  
`sudo apt-get remove docker docker-engine docker.io containerd runc`  
It’s OK if `apt-get` reports that none of these packages are installed.

### INSTALL DOCKER ENGINE
`sudo apt-get update`  

`sudo apt-get install \
     apt-transport-https \
     ca-certificates \
     curl \
     gnupg \
     lsb-release`  
     
`curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg`
  
`echo \
   "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null`
   
`sudo apt-get update`

`sudo apt-get install docker-ce docker-ce-cli containerd.io`

### Setup postgres database in Docker
Setup a container with a postgres database  
`docker run --name tor-usage-postgres -e POSTGRES_PASSWORD=mysecretpassword -d postgres -p 5432:5432`  
Port is localhost:5432  
User: postgres  
Password: mysecretpassword

### remove existing container
`docker rm -f tor-usage-postgres`
