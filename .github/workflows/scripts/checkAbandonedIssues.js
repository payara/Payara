module.exports = async ({github, context, core}) => {
    const { owner, repo } = context.repo;
    const pendingLabel = "Status: Pending";
    const abandonedLabel = "Status: Abandoned";

    const parsedDays = parseFloat(process.env.daysInterval);
    const timeThreshold = parsedDays * 24 * 60 * 60 * 1000;
    core.debug(`Abandoned interval days is set to ${parsedDays}`);

    // Query all GH issues that are pending and not closed
    const issuesResponse = await github.rest.issues.listForRepo({
        owner,
        repo,
        labels: pendingLabel,
        state: "open",
    });

    for (let issue of issuesResponse.data) {
        const updatedAt = new Date(issue.updated_at).getTime();
        const currentTime = new Date().getTime();
        const updateMessage = `Greetings, 
          It's been more than ${parsedDays} days since we requested more information or an update from you on the details of this issue. Could you provide an update soon, please?
          We're afraid that if we do not receive an update, we'll have to close this issue due to inactivity.`;

        if (currentTime - updatedAt > timeThreshold) {
            await github.rest.issues.update({
                owner,
                repo,
                issue_number: issue.number,
                labels: [abandonedLabel]
            });

            await github.rest.issues.createComment({
                owner,
                repo,
                issue_number: issue.number,
                body: updateMessage
            });
        }
    }
}
