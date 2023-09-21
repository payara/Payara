module.exports = async ({github, context, core}) => {
    const { owner, repo } = context.repo;
    const abandonedLabel = "Status: Abandoned";

    const parsedDays = parseFloat(process.env.daysInterval);
    core.debug(`Abandoned interval days is set to ${parsedDays}`);

    const timeThreshold = parsedDays * 24 * 60 * 60 * 1000;
    const response = await github.rest.issues.listForRepo({
        owner,
        repo,
        labels: abandonedLabel,
        state: "open",
    });

    for (let issue of response.data) {
        const updatedAt = new Date(issue.updated_at).getTime();
        const currentTime = new Date().getTime();
        const updateMessage = `Greetings, 
           It's been more than ${parsedDays} days since this issue was identified as abandoned.
           We have closed this issue due to inactivity, please feel free to re-open it if you have more information to share.`;

        if (currentTime - updatedAt > timeThreshold) {
            await github.rest.issues.createComment({
                owner,
                repo,
                issue_number: issue.number,
                body: updateMessage
            });

            await github.rest.issues.update({
                owner,
                repo,
                issue_number: issue.number,
                labels: [],
                state: "closed"
            });
        }
    }
}
