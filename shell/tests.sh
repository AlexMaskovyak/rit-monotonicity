#!/bin/bash
# Run the Junit tests
# Author: Joseph Pecoraro
# Date: Wednesday May 13, 2009

# Assumes the classpath is already setup.
java org.junit.runner.JUnitCore \
	tests.TestBufferUtils         \
	tests.TestChunker             \
	tests.TestEve                 \
	tests.TestPartIndicator       \
	tests.TestSHA1
