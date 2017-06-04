#!/usr/bin/env bash
kill $(netstat -nlp | cat ./Application.port | awk '{print $7}' | awk -F"/" '{ print $1 }')

