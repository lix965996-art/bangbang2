/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 * Description: Hvigor wrapper script
 */

const path = require('path');
const fs = require('fs');
const os = require('os');
const cp = require('child_process');
const process = require('process');

const hvigorWrapperVersion = '6.0.0';
const hvigorDir = path.dirname(__filename);
const hvigorWrapperJar = path.join(hvigorDir, `hvigor-wrapper-${hvigorWrapperVersion}.jar`);
const projectDir = path.dirname(hvigorDir);

let javaExec = 'java';
if (process.env.JAVA_HOME) {
    javaExec = path.join(process.env.JAVA_HOME, 'bin', 'java' + (os.platform() === 'win32' ? '.exe' : ''));
}

function getHvigorJar() {
    const localHvigorDir = path.join(projectDir, 'hvigor');
    const localHvigorJar = path.join(localHvigorDir, 'hvigor-6.0.0.jar');
    if (fs.existsSync(localHvigorJar)) {
        return localHvigorJar;
    }
    return null;
}

function main() {
    const args = process.argv.slice(2);

    const hvigorJar = getHvigorJar();
    if (hvigorJar && fs.existsSync(hvigorJar)) {
        const classpath = [hvigorWrapperJar, hvigorJar].join(path.delimiter);
        const processArgs = [javaExec, '-classpath', classpath, 'com.huawei.ohos.hvigor.wrapper.HvigorWrapperWrapper', ...args];
        const proc = cp.spawn(processArgs[0], processArgs.slice(1), {
            stdio: 'inherit',
            cwd: projectDir
        });
        proc.on('exit', (code) => {
            process.exit(code);
        });
    } else {
        console.error('Hvigor JAR not found. Please sync project in DevEco Studio first.');
        process.exit(1);
    }
}

main();