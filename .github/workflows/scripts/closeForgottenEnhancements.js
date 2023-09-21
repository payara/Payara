module.exports = async ({github, context, core}) => {
    const {owner, repo} = context.repo;
    // Query all GH issues for Voting

    const votingLabel = "Status: Voting";
    let response = await github.rest.issues.listForRepo({
        owner,
        repo,
        labels: votingLabel,
        state: 'open',
    });
    if (response.data.length === 0) {
        core.debug('No issues marked for voting found. Exiting.');
        return;
    }
    const votingThreshold = process.env.maximumVotingThreshold;
    const parsedDays = parseFloat(votingThreshold);

    let now = new Date().getTime();
    for (let issue of response.data) {
        core.debug(`Processing issue #${issue.number}`);
        core.debug(`Issue was created ${issue.created_at}`);

        let createdDate = new Date(issue.created_at).getTime();
        let daysSinceCreated = (now - createdDate) / 1000 / 60 / 60 / 24;
        let reactions = issue.reactions['+1'];

        core.debug(`Issue +1 reactions count is ${reactions}`);

        if (reactions < 2 && daysSinceCreated > parsedDays) {
            core.debug(`Closing #${issue.number} because it hasn't received enough votes after ${parsedDays} days`);

            const message = `Greetings,
                  This issue has been open for community voting for more than ${parsedDays} days and sadly it hasn't received enough votes to be considered for its implementation according to our community policies.
                  As there is not enough interest from the community we'll proceed to close this issue.`;

            await github.rest.issues.createComment({
                owner : owner,
                repo : repo,
                issue_number: issue.number,
                body: message
            });

            await github.rest.issues.update({
                owner: owner,
                repo: repo,
                issue_number: issue.number,
                labels: [],
                state: 'closed'
            });

            await github.rest.issues.lock({
                owner : owner,
                repo : repo,
                issue_number : issue.number,
                lock_reason : 'resolved'
            });
        }
    }
}
