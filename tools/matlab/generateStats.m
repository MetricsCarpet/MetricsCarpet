function stats = generateStats(dirName)
%generateReport Create a report for the given directory.
%
%   Example:
%     stats = generateStats(pwd);
%     metrics = computeMetrics(stats);
%     createReport(metrics,pwd)

% Pramod Kumar and Matthew Simoneau
% Copyright 2005-2006 The MathWorks, Inc.

% Get a listing of all files.
if ~isdir(dirName)
    error('"%s" is not a valid directory.',dirName);
end

% Get a listing of all the M-files, including subdirectories.
fileList = getFilenames(dirName);

% Since cloning is cross-file, compute this first.
cloneInfo = getCloneInfo(fileList,dirName);

% Now iterate over each file in fileList.
stats = struct('lines',{},'comments',{},'complexity',{},'help',{},'mlint',{},'clash',{},'clone',{});
for i = 1:size(fileList,2)
    file = fileList{i};
    stats(i,1).filename = removeBase(file,dirName);
    stats(i,1).name = hashFilename(file,dirName);
    [stats(i,1).lines,stats(i,1).comments] = getLineCounts(file);
    stats(i,1).complexity = cyclomaticComplexity(file);
    [stats(i,1).help,stats(i,1).helpmessages] = generatedHelpMetric(file);
    stats(i,1).mlintmessages = getMlintMessages(file);
    stats(i,1).mlint = numel(stats(i,1).mlintmessages);
    [stats(i,1).clash,stats(i,1).clashname] = getClash(file);
    cloneMatches = strmatch(stats(i,1).filename,{cloneInfo.file1},'exact');
    stats(i,1).clone = numel(cloneMatches);
    stats(i,1).cloneInfo = cloneInfo(cloneMatches);
end

%==========================================================================
function s = removeBase(s,base)
s = strrep(s,[base filesep],'');
s = strrep(s,filesep,'/');

%==========================================================================
function s = hashFilename(s,dirName)
s = strrep(s,[dirName filesep],'');
s = strrep(s,'\','_');

%==========================================================================
function filenames = getFilenames(root)
% getFilenames Returns all the M-files this directory and in subdirectories.

% Everything in this folder.
allFiles = dir([root filesep '*']);

% The files we need in this folder.
myFiles = dir([root filesep '*.m']);

% Build a list of all the files matching the regular expression in this folder.
filenames = {};
for k = 1:length(myFiles)
    filenames{k} = [root filesep myFiles(k).name];
end

% Do a recursive call of this function for every sub directory of this folder.
for k = 1:length(allFiles)
    if allFiles(k).isdir && ...
            ~strcmp(allFiles(k).name,'.') && ...
            ~strcmp(allFiles(k).name,'..')
        thisFolder = [root filesep allFiles(k).name];
        filenames = [filenames getFilenames(thisFolder)];
    end
end

%==========================================================================
function [lineCount,commentCount] = getLineCounts(filename)
% Returns the number of lines of code and comments in a file.

% Count code lines.  It does not include the lines of comments or blank
% lines. If a statement is continued on many lines it counts as only one
f = textread(filename,'%s','delimiter','\n','whitespace','','bufsize',4095*10);
[unused,bptok] = xmtok(f);
% The first line of code of a function is not counted in bptok
codeCount = sum(bptok)+1;

% Count comment lines.
percent = regexp(f, '\s*[%]','start', 'once');
linesWithPercent = sum(~cellfun('isempty',percent));
percentInQuotes = regexp(f, '''.*[%].*''','start', 'once');
linesWithPercentInQuotes = sum(~cellfun('isempty',percentInQuotes));
commentCount = linesWithPercent-linesWithPercentInQuotes;

% Sum for total.
lineCount = codeCount + commentCount;

%==========================================================================
function y = cyclomaticComplexity(fileName)
%CYCLOMATICCOMPLEXITY finds max Cyclomatic Complexity of functions in the file
%   y = CYCLOMATICCOMPLEXITY(myFile) calculates the cyclomatic complexity of
%   the function in "fileName". If the file has more than one function in
%   it then CYCLOMATICCOMPLEXITY calculates the cyclomatic complexity of
%   each function and reports the maximum
%
%   For example if you have 2 functions in the file with complexity of 7 and
%   9 this function will report a complexity value of 9

% Note that the "-cyc" flag is still undocumented and will change in a
% future release.
s = mlint('-cyc','-struct',fileName);

lfunctions = {};
lcomplexities = [];

p1 = 'The McCabe complexity of ''(\w+)'' is (\d+)\.';
p2 = 'The McCabe complexity is (\d+)\.';
for i = 1:length(s)
    m1 = regexp(s(i).message,p1,'tokens');
    m2 = regexp(s(i).message,p2,'tokens');
    if ~isempty(m1)
        lfunctions{end+1,1} = m1{1}{1};
        lcomplexities(end+1,1) = str2double(m1{1}{2});
    elseif ~isempty(m2)
        [unused,name,ext] = fileparts(fileName);
        lfunctions{end+1,1} = [name ext];
        lcomplexities(end+1,1) = str2double(m2{1}{1});        
    end
end

if isempty(lcomplexities)
    y = 0;
else
    y = max(lcomplexities);
end

%==========================================================================
function [metric,y] = generatedHelpMetric(fileName)
%This function generates a number for each file based on the help content
%of the file. H1, Help, and Example each could result in 1 warning. so if
%all three are missing 3 is returned. if two are missing 2 is returned etc.

temp = 0;
y = generateHelpInfo(fileName,0);
if(isempty(y.description))
    temp = temp+1;
end
if(isempty(y.example))
    temp = temp+1;
end
if(isempty(y.help))
    temp = temp+1;
end
metric = temp;

%==========================================================================
function helpInfo = generateHelpInfo(fileName, helpSubfunsDisplay)
%HELPRPT  Scan a file or directory for help.
strc = [];
if (helpSubfunsDisplay==1)
    callStrc = getcallinfo(fileName,'subfuns');
else
    callStrc = getcallinfo(fileName,'file');
end
for m = 1:length(callStrc)
    strc(end+1).filename = fileName;
    strc(end).name = callStrc(m).name;
    strc(end).type = callStrc(m).type;
    strc(end).firstLine = callStrc(m).firstline;
    if strcmp(strc(end).type,'subfunction')
        helpStr = helpfunc([strc(end).filename filemarker strc(end).name]);
    else
        helpStr = helpfunc(strc(end).filename);
    end
    strc(end).help = code2html(helpStr);
    % Remove any leading spaces and percent signs
    % Grab all the text up to the first carriage return
    helpTkn = regexp(helpStr,'^\s*%*\s*([^\n]*)\n','tokens','once');
    if isempty(helpTkn)
        strc(end).description = '';
    else
        strc(end).description = helpTkn{1};
    end
    % Now we grep through the function line by line looking for
    % copyright, example, and see-also information. Don't bother
    % looking for these things in a subfunction.
    % NOTE: This will not work for Japanese files
    if ~strcmp(strc(end).type,'subfunction')
        f = textread(fileName,'%s','delimiter','\n','whitespace','','bufsize',4095*10);
        strc(end).example = '';
        for i = 1:length(f)
            % The help report searches all comment lines in the file,
            % even if they occur after the typical "help block" at the
            % top of the file.
            % If there's no comment character, short circuit the loop.
            if isempty(strfind(f{i},'%'))
                continue
            end
            exTkn = regexpi(f{i},'^\s*%(\s*example)','tokens','once');
            if ~isempty(exTkn)
                exampleStr = {' ',exTkn{1}};
                strc(end).exampleLine = i;
                % Loop through and grep the entire example
                % We assume the example ends when there is a blank
                % line or when the comments end.
                exampleCompleteFlag = 0;
                for j = (i+1):length(f)
                    codeTkn = regexp(f{j},'^\s*%(\s*[^\s].*$)','tokens','once');
                    if isempty(codeTkn)
                        exampleCompleteFlag = 1;
                    else
                        exampleStr{end+1} = codeTkn{1};
                    end
                    if exampleCompleteFlag
                        break
                    end
                end
                strc(end).example = sprintf('%s\n',exampleStr{:});
            end
        end
    end
end
helpInfo= strc;


%==========================================================================
function strc = getcallinfo(filename,option)
%GETCALLINFO  Returns called functions and their initial calling lines
%   STRUCT = GETCALLINFO(FILENAME,OPTION)
%   The output structure STRUCT takes the form
%      type:       [ script | function | subfunction ]
%      name:       name of the script, function, or subfunction
%      firstline:  first line of the script, function, or subfunction
%      calls:      calls made by the script, function, or subfunction
%      calllines:  lines from which the above calls were made
%
%   OPTION = [ 'file' | 'subfuns' | 'funlist' ]
%   By default OPTION is set to 'subfuns'
%
%   OPTION = 'file' returns one structure for the entire file, regardless
%   of whether it is a script, a function with no subfunctions, or a
%   function with subfunctions. For a file with subfunctions, the calls
%   for the file includes all external calls made by subfunctions.
%
%   OPTION = 'subfuns' returns an array of structures. The first is for the
%   for the main function followed by all of the subfunctions. This option
%   returns the same result as 'file' for scripts and one-function files.
%
%   OPTION = 'funlist' returns an array of structures similar to the
%   'subfuns' option, but calls and calllines information is not
%   returned, only the list of subfunctions and their first lines.

%   Copyright 1984-2004 The MathWorks, Inc.
%   Ned Gulley

if nargin < 2
    option = 'subfuns';
end

mlintMsg = mlintmex('-calls',filename);

mainFcnHit =  regexp(mlintMsg,'M0 (\d+) \d+ (\w+)','tokens','once');
subFcnHits = regexp(mlintMsg,'S0 (\d+) \d+ (\w+)','tokens');
filenameHit = regexp(filename,'(\w+)\.[mM]?$','tokens','once');
if isempty(filenameHit)
    error('Illegal filename "%s"',filename)
end
shortfilename = filenameHit{1};

strc = [];

% TODO: watch out for nested functions
if isempty(mainFcnHit)
    % File is a script
    strc.type = 'script';
    strc.name = shortfilename;
    hits = regexp(mlintMsg,'U0 (\d+) \d+ (\w+)','tokens');
    strc.firstline = 1;
    strc.calls = cell(length(hits),1);
    strc.calllines = zeros(length(hits),1);
    for n = 1:length(hits)
        strc.calllines(n) = eval(hits{n}{1});
        strc.calls{n} = hits{n}{2};
    end

else
    % File is a function
    strc(1).type = 'function';
    strc(1).name = shortfilename;
    strc(1).firstline = 1;
    callHits = regexp(mlintMsg,'U1 (\d+) \d+ (\w+)','tokens');

    if strcmp(option,'funlist')

        for n = 1:length(subFcnHits)
            strc(n+1).type = 'subfunction';
            strc(n+1).name = subFcnHits{n}{2};
            strc(n+1).firstline = eval(subFcnHits{n}{1});
        end

    elseif strcmp(option,'file')

        strc(1).calls = {};
        strc(1).calllines = [];
        localFuns = {};
        % Get list of all local functions for de-duping
        for n = 1:length(subFcnHits)
            localFuns{end+1} = subFcnHits{n}{2};
        end
        localFunStr = sprintf('%s ',localFuns{:});

        for n = 1:length(callHits)
            callLine = eval(callHits{n}{1});
            call = callHits{n}{2};
            % Only put the call on the list if it's not a local function
            if isempty(strfind(localFunStr,call))
                strc.calllines(end+1) = callLine;
                strc.calls{end+1} = call;
            end
        end

    else

        strc(1).calls = {};
        strc(1).calllines = [];
        for n = 1:length(subFcnHits)
            strc(n+1).type = 'subfunction';
            strc(n+1).name = subFcnHits{n}{2};
            strc(n+1).firstline = eval(subFcnHits{n}{1});
        end

        % Get list of all first lines for figuring which function to associate
        % the call with
        firstLines = [strc.firstline];

        for n = 1:length(callHits)
            callLine = eval(callHits{n}{1});
            call = callHits{n}{2};
            lineCompare = find(callLine > firstLines);
            strc(lineCompare(end)).calllines(end+1) = callLine;
            strc(lineCompare(end)).calls{end+1} = call;
        end

    end
end

%==========================================================================
function msgs = getMlintMessages(file)

% Get all the M-lint messages for this file.
msgs = mlint(file);

% Strip out messages taht can't be avoided.
ignore = strmatch('The value assigned here to variable ',{msgs.message});
msgs(ignore) = [];
ignore = ~cellfun('isempty',regexp({msgs.message},'Input variable ''.*?'' appears never to be used\.'));
msgs(ignore) = [];
ignore = ~cellfun('isempty',regexp({msgs.message},'Function ''.*?'' appears never to be used\.'));
msgs(ignore) = [];

% Strip out this one, as it makes examples look bad.
ignore = strmatch('Terminate statement with semicolon to suppress output.',{msgs.message});
msgs(ignore) = [];

%==========================================================================
function [clash,clashname] = getClash(file)

[path,filename,ext] = fileparts(file);
fname = [filename ext];

if any(path == '@')
    % Ignore methods.
    clashname = '';

elseif strcmpi(fname,'Contents.m') || strcmpi(fname,'Readme.m')
    % These are OK.
    clashname = '';

else
    % Locate.
    clashname = which(fname);

    % Exclude files in this directory.
    helperdir = fileparts(which(mfilename));
    if strmatch(helperdir,clashname)
        clashname = '';
    end

    % Remove MATLABROOT and normalize filesep.
    clashname = strrep(strrep(clashname,[matlabroot filesep],''),filesep,'/');

    % Exclude methods.
    if any(clashname == '@')
        clashname = '';
    end
    
end

% Define the first output.
clash = ~isempty(clashname);


%==========================================================================
function details = getCloneInfo(fileList,dirname)
%getCloneInfo Returns the cloned code info.
%   getCloneInfo finds cloned code for fileList relative to dirname.

% Create the empty return structure.
details = struct('file1',{},'start1',{},'end1',{},'file2',{},'start2',{},'end2',{});

if ~ispc && ~strcmp(computer,'GLNX86')
    % The executible only runs on some platforms.
    return
end

% Create a temporary file for mdf.exe to act on.
tempfile = tempname;
fid = fopen(tempfile, 'w+');
for i = 1:size(fileList,2)
    fprintf(fid,'%s\n',fileList{i});
end
fclose(fid);

% Call the external executable.
thisDir = fileparts(mfilename('fullpath'));
mdfExe = fullfile(thisDir,'mdf');
[status,out] = system(['"' mdfExe '" "' tempfile '"']);
if status ~= 0
    error(out)
end
delete(tempfile)

% Parse the output.
pattern = '(\S+\.m):\((\d+)-(\d+))\s+MATCHES\s+(\S+\.m):\((\d+)-(\d+))';
info = regexp(out,pattern,'tokens');

% Translate the matches into a structure.
for i = 1:size(info,2)
    details(end+1).file1 = removeBase(info{i}{1},dirname);
    details(end).start1 = str2double(info{i}{2});
    details(end).end1 = str2double(info{i}{3});
    details(end).file2 = removeBase(info{i}{4},dirname);
    details(end).start2 = str2double(info{i}{5});
    details(end).end2 = str2double(info{i}{6});
end
