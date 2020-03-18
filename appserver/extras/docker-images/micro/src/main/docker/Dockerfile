FROM @docker.basicJdkImage@

# Default payara ports to expose
EXPOSE 6900 8080

# Configure environment variables
ENV PAYARA_HOME=/opt/payara\
    DEPLOY_DIR=/opt/payara/deployments

# Create and set the Payara user and working directory owned by the new user
RUN true \
    && groupadd -g 1000 payara \
    && useradd -u 1000 -M -s /bin/bash -d ${PAYARA_HOME} payara -g payara \
    && echo payara:payara | chpasswd \
    && mkdir -p ${DEPLOY_DIR}  \
    && chown -R payara:payara ${PAYARA_HOME} \
    && true

USER payara
WORKDIR ${PAYARA_HOME}

COPY maven/artifacts/payara-micro.jar .

# Default command to run
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=90.0", "-jar", "payara-micro.jar"]
CMD ["--deploymentDir", "/opt/payara/deployments"]