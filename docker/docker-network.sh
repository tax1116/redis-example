#!/bin/bash

if docker network create redis-example-network; then
  echo "Docker 네트워크 'redis-example-network' 성공적으로 생성되었습니다."
else
  echo "Docker 네트워크 생성 중 오류가 발생했습니다."
fi