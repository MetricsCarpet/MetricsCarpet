function [ScreenWidth,ScreenHeight] = CPscreensize
% CPSCREENSIZE Calculate screen dimensions of primary display

% $Revision: 5791 $

MonPos = get(0,'MonitorPositions');
ScreenWidth = MonPos(1,3);
ScreenHeight = MonPos(1,4);