SRC=./src/*
RES=./resources/*
PHONE_IP=192.168.1.12

all: install

clean:
	pebble clean

build: $(SRC) $(RES)
	pebble build

install: build
	pebble install --phone $(PHONE_IP)

debug: build
	pebble install --logs --phone $(PHONE_IP)

log: 
	pebble logs --phone $(PHONE_IP)

