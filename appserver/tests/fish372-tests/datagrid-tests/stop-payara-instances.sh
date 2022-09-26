#!/bin/bash
ps aux | grep -i payara | grep -v grep | awk '{print $2}' | xargs kill -9