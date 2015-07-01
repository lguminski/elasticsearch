package org.apache.mesos.elasticsearch.scheduler;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.apache.commons.lang.NumberUtils.stringToInt;

/**
 * Application which starts the Elasticsearch scheduler
 */
public class Main {

    public static final String NUMBER_OF_HARDWARE_NODES = "n";

    public static final String ZK_HOST = "zk";

    public static final String MANAGEMENT_API_PORT = "m";

    private Options options;

    private Configuration configuration;

    public Main() {
        this.options = new Options();
        this.options.addOption(NUMBER_OF_HARDWARE_NODES, "numHardwareNodes", true, "number of hardware nodes");
        this.options.addOption(ZK_HOST, "ZookeeperNode", true, "Zookeeper IP address and port");
        this.options.addOption(MANAGEMENT_API_PORT, "StatusPort", true, "TCP port for status interface. Default is 8080");
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.run(args);
    }

    public void run(String[] args) {
        parseCommandlineOptions(args);

        final ElasticsearchScheduler scheduler = new ElasticsearchScheduler(configuration, new TaskInfoFactory());

        new SpringApplicationBuilder(WebApplication.class)
                .initializers(applicationContext -> applicationContext.getBeanFactory().registerSingleton("scheduler", scheduler))
                .initializers(applicationContext -> applicationContext.getBeanFactory().registerSingleton("configuration", configuration))
                .showBanner(false)
                .run(args);

        scheduler.run();
    }

    private void parseCommandlineOptions(String[] args) {
        configuration = new Configuration();

        try {
            CommandLineParser parser = new BasicParser();
            CommandLine cmd = parser.parse(options, args);

            String numberOfHwNodesString = cmd.getOptionValue(NUMBER_OF_HARDWARE_NODES);
            String zkHost = cmd.getOptionValue(ZK_HOST);

            if (numberOfHwNodesString == null || zkHost == null) {
                printUsageAndExit();
            }

            configuration.setVersion(getClass().getPackage().getImplementationVersion());
            configuration.setNumberOfHwNodes(Integer.parseInt(numberOfHwNodesString));
            configuration.setZookeeperHost(zkHost);
            configuration.setState(new State(new ZooKeeperStateInterfaceImpl(zkHost + ":" + configuration.getZookeeperPort())));
            configuration.setManagementApiPort(stringToInt(cmd.getOptionValue(MANAGEMENT_API_PORT), 8080));

        } catch (ParseException | IllegalArgumentException e) {
            printUsageAndExit();
        }
    }

    private void printUsageAndExit() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(configuration.getFrameworkName(), options);
        System.exit(2);
    }

}
