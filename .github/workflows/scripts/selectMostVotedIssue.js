module.exports = async ({github, context, core}) => {
    let { owner, repo } = context.repo;

    const openLabel = "Status: Open";
    const votingLabel = "Status: Voting";

    // Query all GH issues for Voting
    const response = await github.rest.issues.listForRepo({
        owner,
        repo,
        labels: votingLabel,
        state: 'open',
        direction: 'desc',
    });

    //response has all the issues labeled with Voting.
    if (response.data.length === 0) {
        core.debug('No issues marked for voting found. Exiting.');
        return;
    }
    //filter issues with at least 2 votes.
    response.data = response.data.filter((issue) => issue.reactions['+1'] > 1)
    if (response.data.length === 0) {
        core.debug('No issues with more than 2 votes found. Exiting');
        return;
    }

    let mostVotes = 0;
    let selectedIssue = 0;
    let oldestDate = null;

    for (const issue of response.data) {
        core.debug(`Processing issue #${issue.number}`);
        core.debug(`Number of +1 reactions ${issue.reactions['+1']}`);
        core.debug(`Issue was created ${issue.created_at}`);

        let votes = issue.reactions['+1'];
        let createdDate = new Date(issue.created_at).getTime();

        if (oldestDate === null) {
            oldestDate = createdDate;
            selectedIssue = issue;
            mostVotes = votes;
        }
        if ((votes >=  mostVotes) && (createdDate < oldestDate)) {
            mostVotes = votes;
            selectedIssue = issue;
        }
    }
    core.debug(`Highest votes is  ${mostVotes}`);
    core.debug(`Final issue selected for enhancement is #${selectedIssue.number} created on ${selectedIssue.created_at}`);

    let message = `Greetings, 
       This enhancement request has been selected by the Payara Community as the most voted enhancement of this month and
       thus will be escalated to our product development backlog.`;

    await github.rest.issues.createComment({
        owner : owner,
        repo : repo,
        issue_number: selectedIssue.number,
        body: message
    });
    await github.rest.issues.update({
        owner : owner,
        repo : repo,
        issue_number: selectedIssue.number,
        labels : [openLabel]
    });

    return {
        number : selectedIssue.number,
        title : selectedIssue.title,
        url: selectedIssue.html_url,
        assignee: selectedIssue.assignees.length ? selectedIssue.assignees[0].login : null
    };
}
