package fish.payara.test.containers.tools.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Manager for preparation and startup of the docker images.
 *
 * @author David Matějček
 */
public abstract class DockerImageManager {

    private static final Logger LOG = LoggerFactory.getLogger(DockerImageManager.class);

    private final String imgNameDownloaded;
    private final String imgNamePrepared;
    private final Network network;


    /**
     * @param imgNameDownloaded basic image to enhance. Must not be null and must exist.
     * @param network can be null.
     */
    public DockerImageManager(final String imgNameDownloaded, final Network network) {
        LOG.debug("DockerImageManager(imgNameDownloaded={}, network={})", imgNameDownloaded, network);
        this.imgNameDownloaded = Objects.requireNonNull(imgNameDownloaded, "imgNameDownloaded");
        this.imgNamePrepared = getNewImageName(imgNameDownloaded);
        this.network = network;
    }


    /**
     * @return command used to enhance the basic image.
     */
    public abstract String getInstallCommand();


    /**
     * @return running instance of the container.
     */
    public abstract GenericContainer<?> start();


    /**
     * @return basic image to enhance. Never null.
     */
    protected String getNameOfPreparedImage() {
        return this.imgNamePrepared;
    }


    /**
     * @return network used by the container. May be null then docker's default network will be
     *         used.
     */
    protected Network getNetwork() {
        return this.network;
    }


    /**
     * Prepares image if it does not exist yet. If it does and forceNew is false, does nothing.
     * If the forceNew is true, deletes existing image and prepares new from downloaded image.
     *
     * @param forceNew
     */
    public void prepareImage(final boolean forceNew) {
        LOG.debug("prepareImage(forceNew={})", forceNew);
        if (forceNew) {
            deleteImageIfExists(this.imgNamePrepared);
        } else {
            final ImageFromDockerfile image = tryToLoadDockerImage(this.imgNamePrepared);
            if (image != null) {
                return;
            }
        }
        createPreparedImage();
    }


    private static String getNewImageName(final String originalImageName) {
        return originalImageName.replaceAll("\\:", "\\-").concat("-for-payara-tests:latest");
    }


    private void deleteImageIfExists(final String imageName) {
        LOG.debug("deleteImageIfExists(imageName={})", imageName);
        final DockerClient dockerClient = DockerClientFactory.instance().client();
        if (dockerClient.listImagesCmd().exec().stream().anyMatch(img -> {
            LOG.trace("Found image with tags: {}", (Object) img.getRepoTags());
            return Arrays.asList(img.getRepoTags()).contains(imageName);
        })) {
            LOG.warn("Removing cached image '{}' ...", imageName);
            dockerClient.removeImageCmd(imageName).exec();
        }
    }


    private ImageFromDockerfile tryToLoadDockerImage(final String imageName) {
        LOG.debug("tryToLoadDockerImage(imageName={})", imageName);
        try {
            final ImageFromDockerfile image = new ImageFromDockerfile(imageName, false)
                .withDockerfileFromBuilder(builder -> {
                    builder.from(imageName).build();
                });
            final String result = image.get();
            LOG.info("Found image: {}", result);
            return image;
        } catch (final InterruptedException | ExecutionException | DockerClientException e) {
            LOG.warn("I could not load the cached image, I will try to download fresh from the network", e);
            return null;
        }
    }


    private void createPreparedImage() {
        LOG.debug("createPreparedImage()");

        final ImageFromDockerfile image = new ImageFromDockerfile(this.imgNamePrepared, false) //
            .withDockerfileFromBuilder(builder -> {
                builder.from(this.imgNameDownloaded).run(getInstallCommand()).build();
            });
        try {
            final String result = image.get();
            LOG.info("Image created: {}", result);
            return;
        } catch (final InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Could not create an image '" + this.imgNamePrepared + "' for tests!", e);
        }
    }
}
