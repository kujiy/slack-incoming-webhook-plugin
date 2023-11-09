# Contributing

## Configuring your workstation

### Required tools

- Visual Studio Code to edit the project.
- OpenJDK 1.8.x or more than.

## Before create issue

Issues which was created have problem. It almost can't reproduce by me.

- Your server environment.
- Software and plugin versions, java environment.
- Other enabled plugins on rundeck.
- Log: rundeck/service.log

There is a good way of resolve issue. It's you put reproducible environment with issue, such as Vagrantfile, Dockerfile, or chef recipes.

## Pull Request Process

1. Ensure any install or build dependencies are removed before the end of the layer when doing a build.
2. Update the `README.md` with details of changes to the interface, this includes new environment variables, exposed ports, useful file locations and container parameters.
3. Change the `project.version` into [`build.gradle`](./build.gradle) file. **The versioning scheme we use is SemVer.**
4. You may merge the Pull Request in once you have the sign-off.

## Git Workflow

We use the workflow with [GitFlow](http://nvie.com/posts/a-successful-git-branching-model/) branchs.

## Commit Messages

Use the specification [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) with that adaptations:

```bash
<type> [@username]: <description>

[optional body]

[optional footer(s)]
```
