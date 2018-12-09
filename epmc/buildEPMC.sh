#!/bin/bash

sudo apt-get update
sudo apt-get install git maven make g++ default-jdk
git clone https://github.com/liyi-david/ePMC.git
cd ePMC
./build.sh
cp epmc-standard.jar ~
cd ~
